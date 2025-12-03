package com.screenleads.backend.app.infraestructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Swagger/OpenAPI documentation.
 * This configuration is only active when springdoc is enabled.
 * In production, Swagger should be disabled for security reasons.
 * 
 * NOTE: This configuration is DISABLED because OpenApiConfig already provides OpenAPI bean.
 * If you need to customize OpenAPI, edit OpenApiConfig.java instead.
 */
@Configuration
@ConditionalOnProperty(name = "swagger.security.config.enabled", havingValue = "true", matchIfMissing = false)
public class SwaggerSecurityConfig {

        @Value("${spring.application.name:ScreenLeads API}")
        private String applicationName;

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title(applicationName + " - REST API")
                                                .version("2.0")
                                                .description("ScreenLeads Backend API Documentation")
                                                .contact(new Contact()
                                                                .name("ScreenLeads Team")
                                                                .email("support@screenleads.com"))
                                                .license(new License()
                                                                .name("Proprietary")
                                                                .url("https://screenleads.com/license")))
                                .components(new Components()
                                                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")
                                                                .description("JWT authentication token"))
                                                .addSecuritySchemes("api-key", new SecurityScheme()
                                                                .type(SecurityScheme.Type.APIKEY)
                                                                .in(SecurityScheme.In.HEADER)
                                                                .name("X-API-Key")
                                                                .description("API Key for external integrations")))
                                .addSecurityItem(new SecurityRequirement()
                                                .addList("bearer-jwt")
                                                .addList("api-key"));
        }
}
