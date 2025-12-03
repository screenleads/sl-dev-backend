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
