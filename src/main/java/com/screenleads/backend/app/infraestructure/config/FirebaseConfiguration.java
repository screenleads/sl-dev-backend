package com.screenleads.backend.app.infraestructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfiguration.class);

    @Value("${firebase.credentials.base64}")
    private String base64Key;

    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    @PostConstruct
    public void init() throws IOException {
        log.info("🔥 Iniciando configuración de Firebase...");
        
        if (base64Key == null || base64Key.isEmpty()) {
            log.error("❌ Error: firebase.credentials.base64 no está configurado");
            throw new RuntimeException("Missing firebase.credentials.base64 configuration");
        }

        log.info("📦 Storage Bucket: {}", storageBucket);

        byte[] decoded = Base64.getDecoder().decode(base64Key);
        InputStream serviceAccount = new ByteArrayInputStream(decoded);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket(storageBucket)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
            log.info("✅ Firebase inicializado correctamente");
        } else {
            log.info("ℹ️ Firebase ya estaba inicializado");
        }
    }
}
