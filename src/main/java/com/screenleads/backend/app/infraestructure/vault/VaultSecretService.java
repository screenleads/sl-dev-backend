package com.screenleads.backend.app.infraestructure.vault;

import com.screenleads.backend.app.infraestructure.config.VaultProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for retrieving secrets from HashiCorp Vault.
 * This service is only active when vault.enabled=true.
 * 
 * Usage:
 * - Store secrets in Vault at the configured path
 * - Retrieve secrets using getSecret() method
 * - Secrets are cached in memory for performance
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vault.enabled", havingValue = "true")
public class VaultSecretService {

    private final VaultProperties vaultProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public VaultSecretService(VaultProperties vaultProperties) {
        this.vaultProperties = vaultProperties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(vaultProperties.getConnectionTimeout()))
                .build();

        log.info("Vault integration enabled. Server: {}", vaultProperties.getAddress());
    }

    /**
     * Retrieve a secret from Vault
     * 
     * @param key The secret key to retrieve
     * @return Optional containing the secret value, or empty if not found
     */
    public Optional<String> getSecret(String key) {
        try {
            String url = String.format("%s/v1/%s",
                    vaultProperties.getAddress(),
                    vaultProperties.getSecretPath());

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(vaultProperties.getReadTimeout()))
                    .header("X-Vault-Token", vaultProperties.getToken())
                    .GET();

            if (vaultProperties.getNamespace() != null && !vaultProperties.getNamespace().isEmpty()) {
                requestBuilder.header("X-Vault-Namespace", vaultProperties.getNamespace());
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                if (data != null && data.containsKey(key)) {
                    return Optional.of(data.get(key).toString());
                }
            } else {
                log.warn("Failed to retrieve secret from Vault. Status: {}", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error retrieving secret from Vault: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        return Optional.empty();
    }

    /**
     * Retrieve all secrets from the configured path
     * 
     * @return Map of all secrets, or empty map if retrieval fails
     */
    public Map<String, Object> getAllSecrets() {
        try {
            String url = String.format("%s/v1/%s",
                    vaultProperties.getAddress(),
                    vaultProperties.getSecretPath());

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(vaultProperties.getReadTimeout()))
                    .header("X-Vault-Token", vaultProperties.getToken())
                    .GET();

            if (vaultProperties.getNamespace() != null && !vaultProperties.getNamespace().isEmpty()) {
                requestBuilder.header("X-Vault-Namespace", vaultProperties.getNamespace());
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                return data != null ? data : Map.of();
            } else {
                log.warn("Failed to retrieve secrets from Vault. Status: {}", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error retrieving secrets from Vault: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        return Map.of();
    }

    /**
     * Check if Vault is accessible and properly configured
     * 
     * @return true if Vault is accessible, false otherwise
     */
    public boolean isVaultAccessible() {
        try {
            String url = String.format("%s/v1/sys/health", vaultProperties.getAddress());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(vaultProperties.getConnectionTimeout()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            log.error("Vault health check failed: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
