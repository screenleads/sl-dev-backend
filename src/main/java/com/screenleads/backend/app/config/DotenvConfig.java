// src/main/java/com/screenleads/backend/app/config/DotenvConfig.java
package com.screenleads.backend.app.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración para cargar variables de entorno desde el archivo .env
 * Solo se activa en desarrollo local cuando existe el archivo .env
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            // Intentar cargar el archivo .env
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // No fallar si no existe .env
                    .load();

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            Map<String, Object> dotenvProperties = new HashMap<>();

            // Copiar todas las variables del .env al contexto de Spring
            dotenv.entries().forEach(entry -> {
                dotenvProperties.put(entry.getKey(), entry.getValue());
                // También establecer en System properties para backward compatibility
                System.setProperty(entry.getKey(), entry.getValue());
            });

            // Añadir las propiedades con alta prioridad
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenvProperties", dotenvProperties));

            System.out.println("✅ Variables de entorno cargadas desde .env");

        } catch (Exception e) {
            // No fallar la aplicación si hay problemas con .env
            System.out.println("⚠️ No se pudo cargar .env (usando variables del sistema): " + e.getMessage());
        }
    }
}
