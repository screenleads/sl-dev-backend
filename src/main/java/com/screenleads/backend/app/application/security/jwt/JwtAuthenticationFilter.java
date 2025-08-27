package com.screenleads.backend.app.application.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT para autenticar peticiones entrantes.
 *
 * - Ignora rutas públicas (login/refresh, swagger, health, WS, OPTIONS).
 * - Extrae el token "Bearer ..." del header Authorization.
 * - Valida el token con JwtService y carga el usuario con UserDetailsService.
 * - Si es válido, fija la autenticación en el SecurityContext.
 * - Si es inválido/ausente, NO lanza excepción: deja que la cadena continúe y
 * que
 * el Security layer responda 401 en rutas que lo requieran.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // Rutas públicas (no requieren autenticación)
    private static final String[] WHITELIST = new String[] {
            // Auth: SOLO login y refresh son públicos (NO /auth/me)
            "/auth/login",
            "/auth/refresh",

            // Swagger / OpenAPI
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",

            // Health
            "/actuator/health",

            // WebSocket público (handshake/info)
            "/chat-socket/**",
            "/ws/status"
    };

    private final JwtService jwtService; // extractUsername(), isTokenValid(), ...
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String uri = request.getRequestURI();
        final String method = request.getMethod();

        try {
            // 0) Preflight siempre pasa
            if (HttpMethod.OPTIONS.matches(method)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 1) Whitelist: deja pasar sin tocar el SecurityContext
            if (isWhitelisted(uri)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2) Header Authorization: Bearer <token>
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                // Sin token -> continúa; si la ruta requiere auth, Security devolverá 401
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(BEARER_PREFIX.length());

            // 3) Extrae username del token
            final String username = jwtService.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 4) Valida token y construye autenticación
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    // Token inválido -> limpia contexto y sigue; endpoint autenticado devolverá 401
                    SecurityContextHolder.clearContext();
                }
            }

            // 5) Continua la cadena
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            // Limpia por seguridad y deja que el chain gestione la respuesta
            SecurityContextHolder.clearContext();
            // No relanzamos RuntimeException para evitar 500: el EntryPoint/AccessDenied se
            // encargará
            filterChain.doFilter(request, response);
        }
    }

    private boolean isWhitelisted(String uri) {
        for (String pattern : WHITELIST) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}
