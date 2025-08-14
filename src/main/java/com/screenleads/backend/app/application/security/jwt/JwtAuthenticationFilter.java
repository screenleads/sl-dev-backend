package com.screenleads.backend.app.application.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
 * - Ignora rutas públicas (auth, swagger, actuator).
 * - Extrae el token "Bearer ..." del header Authorization.
 * - Valida el token con JwtService y carga el usuario con UserDetailsService.
 * - Si es válido, fija la autenticación en el SecurityContext.
 * - Si el token es inválido/expirado, lanza RuntimeException para que
 * lo maneje el CustomAuthenticationEntryPoint con 401 en JSON.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // Rutas públicas (no requieren autenticación)
    private static final String[] WHITELIST = new String[] {
            "/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    private final JwtService jwtService; // Debe proveer extractUsername(), isTokenValid(), etc.
    private final UserDetailsService userDetailsService; // Carga el usuario por username

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        final String requestUri = request.getRequestURI();

        try {
            // 1) Whitelist: deja pasar sin tocar el SecurityContext
            if (isWhitelisted(requestUri)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2) Lee Authorization: Bearer <token>
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                // No hay token -> la cadena continua; si la ruta requiere auth, Security
                // responderá 401
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
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                            null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    throw new RuntimeException("Invalid JWT token");
                }
            }

            // 5) Continua la cadena
            filterChain.doFilter(request, response);

        } catch (RuntimeException ex) {
            // Importante: limpia el contexto y relanza para que lo maneje el EntryPoint
            // (401)
            SecurityContextHolder.clearContext();
            throw ex;
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
