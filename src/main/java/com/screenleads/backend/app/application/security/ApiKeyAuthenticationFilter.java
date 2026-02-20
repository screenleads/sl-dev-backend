package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.application.util.ApiKeyGenerator;
import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import com.screenleads.backend.app.domain.model.ApiClient;
import com.screenleads.backend.app.domain.repositories.ClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final ClientRepository clientRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final RateLimitService rateLimitService;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository,
            ClientRepository clientRepository,
            ApiKeyGenerator apiKeyGenerator,
            RateLimitService rateLimitService) {
        this.apiKeyRepository = apiKeyRepository;
        this.clientRepository = clientRepository;
        this.apiKeyGenerator = apiKeyGenerator;
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Permitir explícitamente la ruta SSO-login sin API-KEY ni client-id
        String uri = request.getRequestURI();
        if (uri.equals("/api/customers/sso-login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Si ya hay autenticación (por ejemplo JWT), no machacamos
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-KEY");
        String clientId = request.getHeader("client_id");
        if (clientId == null || clientId.isEmpty()) {
            clientId = request.getHeader("client-id");
        }

        log.info("🔐 ApiKeyAuthenticationFilter - Path: {}, API-KEY: {}, client-id: {}",
                uri,
                maskApiKey(apiKey),
                clientId);

        if (apiKey != null && clientId != null) {
            Optional<ApiClient> clientOpt = clientRepository.findByClientIdAndActiveTrue(clientId);
            if (clientOpt.isPresent()) {
                ApiClient client = clientOpt.get();
                log.info("✅ Cliente encontrado: ID={}, clientId={}", client.getId(), client.getClientId());

                // Extraer el prefijo de la API key para buscar en BD
                try {
                    String keyPrefix = apiKeyGenerator.extractPrefix(apiKey);
                    log.debug("🔑 Prefijo extraído: {}", keyPrefix);

                    // Buscar la API key por prefijo y cliente
                    Optional<ApiKey> keyOpt = apiKeyRepository.findByKeyPrefixAndApiClientAndActiveTrue(keyPrefix,
                            client);

                    if (keyOpt.isPresent()) {
                        ApiKey key = keyOpt.get();

                        // Verificar que el hash coincida con la key proporcionada
                        if (apiKeyGenerator.matchesHash(apiKey, key.getKeyHash())) {
                            // Validar que no esté expirada ni revocada
                            if (!key.isValid()) {
                                log.warn("❌ API Key inválida (expirada o revocada): keyPrefix={}", keyPrefix);
                            } else {
                                Long apiKeyId = key.getId();
                                boolean isLive = key.isLive();

                                // ⏱️ RATE LIMITING: Verificar límite de peticiones
                                if (!rateLimitService.allowRequest(apiKeyId, isLive)) {
                                    RateLimitService.RateLimitInfo info = rateLimitService.getRateLimitInfo(apiKeyId,
                                            isLive);

                                    log.warn("🚫 Rate limit excedido - apiKeyId: {}, isLive: {}, limit: {}/min",
                                            apiKeyId, isLive, info.getLimit());

                                    // Agregar headers de rate limit
                                    response.setHeader("X-RateLimit-Limit", String.valueOf(info.getLimit()));
                                    response.setHeader("X-RateLimit-Remaining", "0");
                                    response.setHeader("X-RateLimit-Reset", String.valueOf(info.getResetIn()));
                                    response.setHeader("Retry-After", String.valueOf(info.getResetIn()));

                                    response.setStatus(429); // Too Many Requests
                                    response.setContentType("application/json");
                                    response.getWriter().write(String.format(
                                            "{\"error\":\"Rate limit exceeded\",\"message\":\"Límite de %d peticiones/minuto excedido. Reintenta en %d segundos.\",\"limit\":%d,\"resetIn\":%d}",
                                            info.getLimit(), info.getResetIn(), info.getLimit(), info.getResetIn()));
                                    return; // No continuar con el filter chain
                                }

                                // Agregar headers informativos de rate limit
                                RateLimitService.RateLimitInfo info = rateLimitService.getRateLimitInfo(apiKeyId,
                                        isLive);
                                response.setHeader("X-RateLimit-Limit", String.valueOf(info.getLimit()));
                                response.setHeader("X-RateLimit-Remaining", String.valueOf(info.getRemaining()));
                                response.setHeader("X-RateLimit-Reset", String.valueOf(info.getResetIn()));

                                log.info("✅ API Key válida - ID: {}, Type: {}, Scopes: {}, RateLimit: {}/{} remaining",
                                        apiKeyId, isLive ? "LIVE" : "TEST", key.getScopes(),
                                        info.getRemaining(), info.getLimit());

                                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                        apiKeyId,
                                        null,
                                        List.of(new SimpleGrantedAuthority("API_CLIENT")));
                                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authToken);
                            }
                        } else {
                            log.warn("❌ Hash de API Key no coincide: keyPrefix={}", keyPrefix);
                        }
                    } else {
                        log.warn("❌ API Key no encontrada con prefijo: {}", keyPrefix);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("❌ Formato de API Key inválido: {}", e.getMessage());
                }
            } else {
                log.warn("❌ Cliente no encontrado o inactivo con clientId: {}", clientId);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null) {
            return "null";
        }
        int lastFourStart = Math.max(0, apiKey.length() - 4);
        return "***" + apiKey.substring(lastFourStart);
    }
}
