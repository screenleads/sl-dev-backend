package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import com.screenleads.backend.app.domain.model.Client;
import com.screenleads.backend.app.domain.repositories.ClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
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

        if (apiKey != null && clientId != null) {
            Optional<Client> clientOpt = clientRepository.findByClientIdAndActiveTrue(clientId);
            if (clientOpt.isPresent()) {
                Client client = clientOpt.get();
                Optional<ApiKey> keyOpt = apiKeyRepository.findByKeyAndClientAndActiveTrue(apiKey, client);
                
                if (keyOpt.isPresent()) {
                    ApiKey key = keyOpt.get();
                    
                    // Verificar si la API Key ha expirado
                    if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
                        // API Key expirada, no autenticar
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    // Parsear permisos desde el string
                    Set<String> permissions = parsePermissions(key.getPermissions());
                    
                    // Crear el principal con toda la información
                    ApiKeyPrincipal principal = new ApiKeyPrincipal(
                        key.getId(),
                        client.getClientId(),
                        client.getId(),
                        permissions,
                        key.getCompanyScope()
                    );
                    
                    // Crear autenticación con la autoridad API_CLIENT
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("API_CLIENT"))
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Parsea el string de permisos a un Set.
     * Formato esperado: "snapshot:read,snapshot:create,lead:read,lead:update"
     */
    private Set<String> parsePermissions(String permissionsStr) {
        if (permissionsStr == null || permissionsStr.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        return Arrays.stream(permissionsStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }
}
