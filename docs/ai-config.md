# Configuración & Recursos — snapshot incrustado

> application.yml/properties y clases @Configuration.

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/infraestructure/config/FirebaseConfiguration.java
package com.screenleads.backend.app.infraestructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Configuration
public class FirebaseConfiguration {
    @PostConstruct
    public void init() throws IOException {

        String base64Key = System.getenv("GOOGLE_CREDENTIALS_BASE64");
        if (base64Key == null)
            throw new RuntimeException("Missing GOOGLE_CREDENTIALS_BASE64");

        byte[] decoded = Base64.getDecoder().decode(base64Key);
        InputStream serviceAccount = new ByteArrayInputStream(decoded);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket("screenleads-e7e0b.firebasestorage.app")
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
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
spring.application.name=app
server.port=3000
PostgreSQL database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/sl_db
spring.datasource.username=postgres
spring.datasource.password=52866617jJ@

spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configurations
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=200MB

server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true

application.security.jwt.secret-key=KpG2vh6T9X5uN0rA3LqD7zW1mZcB8VtY

logging.level.org.springframework.security=DEBUG


```

```properties
// src/main/resources/application-pre.properties
spring.application.name=app
server.port=3000

spring.datasource.url=${JDBC_DATABASE_URL}
spring.datasource.username=${JDBC_DATABASE_USERNAME}
spring.datasource.password=${JDBC_DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configurations
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=200MB

server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true

application.security.jwt.secret-key=KpG2vh6T9X5uN0rA3LqD7zW1mZcB8VtY


# --- MOSTRAR ERROR EN RESPUESTA (temporal para depurar) ---
# always -> siempre muestra la traza en el body (temporal). Alternativa segura: on_param y llamar con ?trace=true
server.error.include-message=always
server.error.include-binding-errors=always
server.error.whitelabel.enabled=false

# --- LOGS VERBOSOS PARA VER LA CAUSA ---
logging.level.ROOT=INFO
logging.level.com.screenleads=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# (Hibernate 6 cambió algunos loggers; deja ambos para asegurar bind de parámetros)
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
logging.level.org.hibernate.orm.jdbc.bind=TRACE

# (Opcional) Permite que Spring pueda loguear detalles de requests si activas un filtro de logging
spring.mvc.log-request-details=true

```

```properties
// src/main/resources/application-pro.properties
spring.application.name=app
server.port=3000

spring.datasource.url=${JDBC_DATABASE_URL}
spring.datasource.username=${JDBC_DATABASE_USERNAME}
spring.datasource.password=${JDBC_DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configurations
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=200MB

server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true

application.security.jwt.secret-key=KpG2vh6T9X5uN0rA3LqD7zW1mZcB8VtY

logging.level.org.springframework.security=DEBUG
```

```properties
// src/main/resources/application.properties
spring.application.name=app
server.port=3000
# PostgreSQL database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/sl_db
spring.datasource.username=postgres
spring.datasource.password=52866617jJ@
# spring.datasource.url=${JDBC_DATABASE_URL}
# spring.datasource.username=${JDBC_DATABASE_USERNAME}
# spring.datasource.password=${JDBC_DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA configurations
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=200MB

server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true

application.security.jwt.secret-key=KpG2vh6T9X5uN0rA3LqD7zW1mZcB8VtY

logging.level.org.springframework.security=DEBUG
# ==== CORS (separados por coma) ====
cors.allowed-origins=http://localhost:4200,ionic://localhost,capacitor://localhost,https://tu-dominio.com

# ==== Opcional: 404 limpias si no hay handler ====
spring.web.resources.add-mappings=false
```

