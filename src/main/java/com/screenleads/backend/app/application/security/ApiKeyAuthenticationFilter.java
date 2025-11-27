package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-KEY");
        String clientId = request.getHeader("client_id");

        if (apiKey != null && clientId != null) {
            Optional<ApiKey> keyOpt = apiKeyRepository.findByKeyAndClientIdAndActiveTrue(apiKey, clientId);
            if (keyOpt.isPresent()) {
                ApiKey key = keyOpt.get();
                // Puedes adaptar los permisos aquí
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        key.getClientId(), null, Collections.singletonList(new SimpleGrantedAuthority("API_CLIENT")));
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
