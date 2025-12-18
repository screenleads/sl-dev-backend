package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import com.screenleads.backend.app.domain.model.Client;
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

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository, ClientRepository clientRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

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
                request.getRequestURI(),
                maskApiKey(apiKey),
                clientId);

        if (apiKey != null && clientId != null) {
            Optional<Client> clientOpt = clientRepository.findByClientIdAndActiveTrue(clientId);
            if (clientOpt.isPresent()) {
                Client client = clientOpt.get();
                log.info("✅ Cliente encontrado: ID={}, clientId={}", client.getId(), client.getClientId());

                Optional<ApiKey> keyOpt = apiKeyRepository.findByKeyAndClientAndActiveTrue(apiKey, client);
                if (keyOpt.isPresent()) {
                    ApiKey key = keyOpt.get();
                    // Usar el ID de la API key como principal para identificar exactamente qué API
                    // key se está usando
                    Long apiKeyId = key.getId();
                    log.info("✅ API Key válida encontrada - ID: {}, ClientDbId: {}, Permissions: {}",
                            apiKeyId, client.getId(), key.getPermissions());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            apiKeyId,
                            null,
                            List.of(new SimpleGrantedAuthority("API_CLIENT")));
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("✅ Autenticación establecida para apiKeyId: {}", apiKeyId);
                } else {
                    log.warn("❌ API Key no encontrada o inactiva");
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
