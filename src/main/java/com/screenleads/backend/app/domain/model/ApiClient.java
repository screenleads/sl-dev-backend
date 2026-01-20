package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un cliente API (aplicación externa) que consume los servicios.
 * No confundir con Customer (cliente final que canjea promociones).
 */
@Entity
@Table(name = "client")
public class ApiClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, unique = true, length = 100)
    private String clientId; // Identificador único del cliente API

    @Column(nullable = false, length = 255)
    private String name; // Nombre de la aplicación/cliente

    @Column(nullable = false)
    private Boolean active = true;

    // Relación con API Keys
    @OneToMany(mappedBy = "apiClient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiKey> apiKeys = new ArrayList<>();

    // Constructors
    public ApiClient() {
    }

    public ApiClient(String clientId, String name) {
        this.clientId = clientId;
        this.name = name;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<ApiKey> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<ApiKey> apiKeys) {
        this.apiKeys = apiKeys;
    }
}
