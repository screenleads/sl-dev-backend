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
