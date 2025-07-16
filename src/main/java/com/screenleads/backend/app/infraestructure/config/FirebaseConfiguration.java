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
