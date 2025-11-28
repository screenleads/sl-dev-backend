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
import java.util.Collections;
import java.util.Optional;

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
                            // Acceder a clientId aquí para evitar LazyInitializationException
                            String safeClientId = client.getClientId();
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    safeClientId,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("API_CLIENT")));
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
