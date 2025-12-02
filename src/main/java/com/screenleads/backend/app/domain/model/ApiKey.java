package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client", nullable = false)
    private Client client;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    /**
     * Permisos en formato JSON o string separado por comas.
     * Ejemplo: "snapshot:read,snapshot:create,lead:read,lead:update"
     */
    @Column(columnDefinition = "TEXT")
    private String permissions;

    /**
     * Alcance de datos:
     * - NULL o "ALL": acceso a datos de todas las compañías (sin filtro)
     * - ID numérico: acceso solo a datos de esa compañía específica
     */
    @Column(name = "company_scope")
    private Long companyScope;

    /**
     * Descripción o nombre legible de la API Key
     */
    @Column(length = 255)
    private String description;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public Long getCompanyScope() { return companyScope; }
    public void setCompanyScope(Long companyScope) { this.companyScope = companyScope; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
