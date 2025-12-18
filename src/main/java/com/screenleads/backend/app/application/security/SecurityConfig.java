package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.application.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final ApiKeyAuthenticationFilter apiKeyAuthFilter;
        private final UserDetailsService userDetailsService;

        // Respuestas JSON limpias para 401 / 403
        private final CustomAuthenticationEntryPoint authEntryPoint;
        private final CustomAccessDeniedHandler accessDeniedHandler;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(eh -> eh
                                                .authenticationEntryPoint(authEntryPoint) // 401
                                                .accessDeniedHandler(accessDeniedHandler) // 403
                                )
                                .authorizeHttpRequests(auth -> auth
                                                // Preflight
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // WebSocket público (handshake/info) y status
                                                .requestMatchers(HttpMethod.GET, "/chat-socket/**").permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/chat-socket/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/ws/status").permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/ws/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/ws/command/**").authenticated()

                                                .requestMatchers(
                                                                com.screenleads.backend.app.infraestructure.config.SwaggerWhitelist.ENDPOINTS)
                                                .permitAll()

                                                // Auth: solo login/refresh públicos
                                                .requestMatchers("/auth/login", "/auth/refresh").permitAll()
                                                // /auth/me requiere autenticación
                                                .requestMatchers("/auth/me").authenticated()

                                                // Swagger / OpenAPI / Health
                                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers("/actuator/health").permitAll()

                                                // El resto autenticado
                                                .anyRequest().authenticated())
                                .authenticationProvider(authenticationProvider())

                                // ✅ IMPORTANTE: anclar SIEMPRE a un filtro estándar (no a filtros custom)
                                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder());
                provider.setUserDetailsService(userDetailsService);
                return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * CORS global: permite orígenes y *headers* usados por tu interceptor.
         * Importante: como allowCredentials=true, no se puede usar "*" en
         * allowedOrigins.
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                // Orígenes explícitos (ajusta según tus entornos)
                config.setAllowedOrigins(List.of(
                                "https://localhost",
                                "https://localhost:4200",
                                "https://localhost:8100",
                                "http://localhost:4200",
                                "http://localhost:8100",
                                "https://sl-device-connector.web.app",
                                "https://sl-dev-dashboard-pre-c4a3c4b00c91.herokuapp.com",
                                "https://sl-dev-dashboard-bfb13611d5d6.herokuapp.com"
                // añade aquí preprod/prod si procede
                ));

                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

                // Headers que envía el interceptor
                config.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "Origin",
                                "X-Requested-With",
                                "X-Timezone",
                                "X-Timezone-Offset",
                                "Accept-Language",
                                "X-API-KEY",
                                "client_id",
                                "client-id"));

                // (Opcional) Headers expuestos al frontend si necesitas leerlos
                config.setExposedHeaders(List.of(
                                "Authorization",
                                "X-Timezone",
                                "X-Timezone-Offset"));

                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
