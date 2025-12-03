# Configuración & Recursos — snapshot incrustado

> application.yml/properties y clases @Configuration.

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/infraestructure/config/ActuatorSecurityConfig.java
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

```

```java
// src/main/java/com/screenleads/backend/app/infraestructure/config/FirebaseConfiguration.java
package com.screenleads.backend.app.infraestructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseConfiguration {

    @Value("${firebase.credentials.base64}")
    private String base64Key;

    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    @PostConstruct
    public void init() throws IOException {
        if (base64Key == null || base64Key.isEmpty()) {
            throw new RuntimeException("Missing firebase.credentials.base64 configuration");
        }

        byte[] decoded = Base64.getDecoder().decode(base64Key);
        InputStream serviceAccount = new ByteArrayInputStream(decoded);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket(storageBucket)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/infraestructure/config/OpenApiConfig.java
package com.screenleads.backend.app.infraestructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI baseOpenAPI() {
    return new OpenAPI().info(new Info()
        .title("ScreenLeads API")
        .version("v1")).components(new Components().addSecuritySchemes("bearerAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));

  }

  // Grupo "public" (el que consulta tu Swagger UI)
  @Bean
  public GroupedOpenApi publicGroup() {
    return GroupedOpenApi.builder()
        .group("public")
        .packagesToScan("com.screenleads.backend.app.web") // más amplio
        .pathsToMatch("/**") // sin limitar
        .build();
  }
}

```

```java
// src/main/java/com/screenleads/backend/app/infraestructure/config/StripeConfig.java
package com.screenleads.backend.app.infraestructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class StripeConfig {
    @Value("${stripe.secret}")
    private String secret;

    @Bean
    public com.stripe.StripeClient stripeClient() {
        return new com.stripe.StripeClient(secret);
    }
}
```

```java
// src/main/java/com/screenleads/backend/app/infraestructure/config/SwaggerSecurityConfig.java
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

```

```java
// src/main/java/com/screenleads/backend/app/infraestructure/config/SwaggerWhitelist.java
package com.screenleads.backend.app.infraestructure.config;

public final class SwaggerWhitelist {
private SwaggerWhitelist() {}
public static final String[] ENDPOINTS = {
"/v3/api-docs/**",
"/swagger-ui.html",
"/swagger-ui/**"
};
}
```

```java
// src/main/java/com/screenleads/backend/app/infraestructure/config/VaultProperties.java
package com.screenleads.backend.app.infraestructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for HashiCorp Vault integration.
 * Allows secure external secrets management.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "vault")
public class VaultProperties {

    /**
     * Enable/Disable Vault integration
     */
    private boolean enabled = false;

    /**
     * Vault server address (e.g., http://localhost:8200)
     */
    private String address;

    /**
     * Vault authentication token
     */
    private String token;

    /**
     * Secret path in Vault (e.g., secret/screenleads)
     */
    private String secretPath;

    /**
     * Vault namespace (optional, for Vault Enterprise)
     */
    private String namespace;

    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 5000;

    /**
     * Read timeout in milliseconds
     */
    private int readTimeout = 15000;
}

```

```java
// src/main/java/com/screenleads/backend/app/infraestructure/config/WebSocketConfiguration.java
package com.screenleads.backend.app.infraestructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.config.ChannelRegistration;
import com.screenleads.backend.app.infraestructure.websocket.PresenceChannelInterceptor;
import com.screenleads.backend.app.application.security.websocket.AuthChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private PresenceChannelInterceptor presenceChannelInterceptor;

    @Autowired
    private AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Interceptores de presencia y autenticación JWT
        registration.interceptors(presenceChannelInterceptor, authChannelInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat-socket")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "https://localhost",
                        "http://localhost:4200",
                        "http://localhost:8100",
                        "https://sl-device-connector.web.app",
                        "https://sl-dev-dashboard-pre-c4a3c4b00c91.herokuapp.com",
                        "https://sl-dev-dashboard-bfb13611d5d6.herokuapp.com")
                .withSockJS();
    }
}

```

```properties
// src/main/resources/application-dev.properties
# ==============================================================================
# DEVELOPMENT ENVIRONMENT CONFIGURATION
# ==============================================================================
spring.application.name=app
server.port=3000

# ==============================================================================
# DATABASE CONFIGURATION - DEV
# ==============================================================================
spring.datasource.url=${JDBC_DATABASE_URL:jdbc:postgresql://localhost:5432/sl_db}
spring.datasource.username=${JDBC_DATABASE_USERNAME:postgres}
spring.datasource.password=${JDBC_DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configurations
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# ==============================================================================
# FILE UPLOAD
# ==============================================================================
spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=200MB

# ==============================================================================
# ENCODING
# ==============================================================================
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true

# ==============================================================================
# JWT SECURITY
# ==============================================================================
application.security.jwt.secret-key=${JWT_SECRET_KEY}
application.security.jwt.expiration=${JWT_EXPIRATION:86400000}

# ==============================================================================
# LOGGING - DEVELOPMENT (VERBOSE)
# ==============================================================================
logging.level.ROOT=INFO
logging.level.com.screenleads=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
logging.level.org.hibernate.orm.jdbc.bind=TRACE

# ==============================================================================
# ACTUATOR - DEVELOPMENT (ALL ENABLED)
# ==============================================================================
management.endpoints.enabled-by-default=true
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always

# ==============================================================================
# SWAGGER - DEVELOPMENT (ENABLED)
# ==============================================================================
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true


```

```properties
// src/main/resources/application-pre.properties
# ==============================================================================
# PRE-PRODUCTION ENVIRONMENT CONFIGURATION
# ==============================================================================
spring.application.name=app
server.port=3000

# ==============================================================================
# DATABASE CONFIGURATION - PRE
# ==============================================================================
spring.datasource.url=${JDBC_DATABASE_URL}
spring.datasource.username=${JDBC_DATABASE_USERNAME}
spring.datasource.password=${JDBC_DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configurations
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# ==============================================================================
# FILE UPLOAD
# ==============================================================================
spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=200MB

# ==============================================================================
# ENCODING
# ==============================================================================
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true

# ==============================================================================
# JWT SECURITY
# ==============================================================================
application.security.jwt.secret-key=${JWT_SECRET_KEY}
application.security.jwt.expiration=${JWT_EXPIRATION:86400000}

# ==============================================================================
# LOGGING - PRE-PRODUCTION
# ==============================================================================
logging.level.ROOT=INFO
logging.level.com.screenleads=INFO
logging.level.org.springframework.security=INFO
logging.level.org.springframework.web=WARN

# ==============================================================================
# ERROR HANDLING
# ==============================================================================
server.error.include-message=always
server.error.include-binding-errors=always
server.error.whitelabel.enabled=false
spring.mvc.log-request-details=true

# ==============================================================================
# ACTUATOR - PRE-PRODUCTION (LIMITED)
# ==============================================================================
management.endpoints.enabled-by-default=false
management.endpoint.health.enabled=true
management.endpoint.info.enabled=true
management.endpoint.metrics.enabled=true
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized

# ==============================================================================
# SWAGGER - PRE-PRODUCTION (ENABLED WITH AUTH)
# ==============================================================================
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true

```

```properties
// src/main/resources/application-pro.properties
# ==============================================================================
# PRODUCTION ENVIRONMENT CONFIGURATION
# ==============================================================================
spring.application.name=app
server.port=3000

# ==============================================================================
# DATABASE CONFIGURATION - PRODUCTION
# ==============================================================================
spring.datasource.url=${JDBC_DATABASE_URL}
spring.datasource.username=${JDBC_DATABASE_USERNAME}
spring.datasource.password=${JDBC_DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configurations
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# ==============================================================================
# FILE UPLOAD
# ==============================================================================
spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=200MB

# ==============================================================================
# ENCODING
# ==============================================================================
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true

# ==============================================================================
# JWT SECURITY
# ==============================================================================
application.security.jwt.secret-key=${JWT_SECRET_KEY}
application.security.jwt.expiration=${JWT_EXPIRATION:86400000}

# ==============================================================================
# LOGGING - PRODUCTION (MINIMAL)
# ==============================================================================
logging.level.ROOT=WARN
logging.level.com.screenleads=INFO
logging.level.org.springframework.security=WARN
logging.level.org.springframework.web=WARN

# ==============================================================================
# ERROR HANDLING - PRODUCTION (SECURE)
# ==============================================================================
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-stacktrace=never
server.error.include-exception=false
server.error.whitelabel.enabled=false

# ==============================================================================
# ACTUATOR - PRODUCTION (MINIMAL & SECURED)
# ==============================================================================
management.endpoints.enabled-by-default=false
management.endpoint.health.enabled=true
management.endpoint.info.enabled=true
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=never
management.endpoint.health.show-components=never

# ==============================================================================
# SWAGGER - PRODUCTION (DISABLED)
# ==============================================================================
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

```properties
// src/main/resources/application.properties
# ==============================================================================
# APPLICATION CONFIGURATION
# ==============================================================================
spring.application.name=app
server.port=${SERVER_PORT:3000}

# ==============================================================================
# FRONTEND & CORS
# ==============================================================================
app.frontendUrl=${APP_FRONTEND_URL:http://localhost:4200}
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:4200,ionic://localhost,capacitor://localhost}

# ==============================================================================
# DATABASE CONFIGURATION
# ==============================================================================
spring.datasource.url=${JDBC_DATABASE_URL:jdbc:postgresql://localhost:5432/sl_db}
spring.datasource.username=${JDBC_DATABASE_USERNAME:postgres}
spring.datasource.password=${JDBC_DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configurations
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=${JPA_SHOW_SQL:false}
spring.jpa.properties.hibernate.format_sql=true

# ==============================================================================
# FILE UPLOAD
# ==============================================================================
spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=200MB

# ==============================================================================
# ENCODING
# ==============================================================================
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true

# ==============================================================================
# STRIPE CONFIGURATION
# ==============================================================================
stripe.secret=${STRIPE_SECRET_KEY}
stripe.priceId=${STRIPE_PRICE_ID}
stripe.webhookSecret=${STRIPE_WEBHOOK_SECRET}

# ==============================================================================
# FIREBASE CONFIGURATION
# ==============================================================================
firebase.enabled=${FIREBASE_ENABLED:false}
firebase.credentials.base64=${GOOGLE_CREDENTIALS_BASE64:}
firebase.storage.bucket=${FIREBASE_STORAGE_BUCKET:screenleads-e7e0b.firebasestorage.app}

# ==============================================================================
# JWT SECURITY
# ==============================================================================
application.security.jwt.secret-key=${JWT_SECRET_KEY}
application.security.jwt.expiration=${JWT_EXPIRATION:86400000}

# ==============================================================================
# LOGGING
# ==============================================================================
logging.level.ROOT=${LOGGING_LEVEL_ROOT:INFO}
logging.level.com.screenleads=${LOGGING_LEVEL_APP:INFO}
logging.level.org.springframework.security=${LOGGING_LEVEL_SECURITY:INFO}
logging.level.org.springframework.web=${LOGGING_LEVEL_WEB:INFO}

# ==============================================================================
# ACTUATOR & MONITORING
# ==============================================================================
management.endpoints.enabled-by-default=false
management.endpoint.health.enabled=true
management.endpoint.info.enabled=true
management.endpoint.metrics.enabled=${ACTUATOR_METRICS_ENABLED:false}
management.endpoints.web.exposure.include=${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE:health,info}
management.endpoint.health.show-details=when-authorized

# ==============================================================================
# SWAGGER / OPENAPI
# ==============================================================================
springdoc.api-docs.enabled=${SWAGGER_ENABLED:true}
springdoc.swagger-ui.enabled=${SWAGGER_ENABLED:true}
springdoc.swagger-ui.path=/swagger-ui

# ==============================================================================
# WEB RESOURCES
# ==============================================================================
spring.web.resources.add-mappings=false

# ==============================================================================
# VAULT CONFIGURATION (Optional)
# ==============================================================================
vault.enabled=${VAULT_ENABLED:false}
vault.address=${VAULT_ADDR:http://localhost:8200}
vault.token=${VAULT_TOKEN:}
vault.secret-path=${VAULT_SECRET_PATH:secret/screenleads}
```

