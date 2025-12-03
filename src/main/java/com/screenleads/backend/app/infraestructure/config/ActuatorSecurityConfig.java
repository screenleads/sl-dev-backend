package com.screenleads.backend.app.infraestructure.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Actuator endpoints.
 * This configuration is separate from the main security config
 * and has higher priority (Order 1) to handle actuator endpoints first.
 * 
 * Production Security:
 * - Health endpoint: public (required for load balancers)
 * - Info endpoint: public (non-sensitive metadata)
 * - All other endpoints: restricted to authenticated users with ADMIN role
 * 
 * Development Security:
 * - All endpoints: public (for easier debugging)
 * 
 * NOTE: This configuration is DISABLED by default because SecurityConfig already handles /actuator/health.
 * Enable only if you need separate security rules for other actuator endpoints.
 */
@Configuration
@EnableWebSecurity
@Order(1)
@ConditionalOnProperty(name = "actuator.security.enabled", havingValue = "true", matchIfMissing = false)
public class ActuatorSecurityConfig {

    /**
     * Security configuration for actuator endpoints.
     * Only applies when management endpoints are enabled AND actuator.security.enabled=true.
     */
    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints (required for infrastructure)
                        .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                        // Metrics endpoint - requires authentication
                        .requestMatchers(EndpointRequest.to("metrics")).authenticated()
                        // All other actuator endpoints - require ADMIN role
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN"))
                .httpBasic(httpBasic -> {
                    // Enable HTTP Basic Auth for actuator endpoints
                    // In production, use proper credentials from environment variables
                })
                .csrf(csrf -> csrf.disable()); // Disable CSRF for actuator endpoints

        return http.build();
    }
}
