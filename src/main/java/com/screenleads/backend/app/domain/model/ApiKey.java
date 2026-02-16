package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SEGURIDAD: Ya no almacenamos la key en texto plano
    // Solo guardamos el hash BCrypt de la key
    @Column(name = "key_hash", nullable = false, length = 60)
    private String keyHash;

    // Prefijo visible de la key (primeros 12 caracteres) para identificarla
    // Ejemplo: "sk_live_abc1"
    @Column(name = "key_prefix", nullable = false, length = 15)
    private String keyPrefix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client", nullable = false)
    private ApiClient apiClient;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // Scopes separados por coma: "customers:read,customers:write,campaigns:read"
    @Column(length = 500)
    private String scopes;

    @Column(name = "company_scope")
    private Long companyScope; // NULL = acceso global, ID = compañía específica

    private String name; // Nombre descriptivo de la API Key
    
    @Column(length = 1000)
    private String description; // Descripción de la API Key

    // ===== CAMPOS DE REVOCACIÓN =====
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    // Contador de usos
    @Column(name = "usage_count")
    private Integer usageCount = 0;

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public Long getCompanyScope() {
        return companyScope;
    }

    public void setCompanyScope(Long companyScope) {
        this.companyScope = companyScope;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public void setRevokedReason(String revokedReason) {
        this.revokedReason = revokedReason;
    }

    public User getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(User revokedBy) {
        this.revokedBy = revokedBy;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    /**
     * Verifica si la API key está revocada
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Verifica si la API key ha expirado
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verifica si la API key es válida (activa, no revocada, no expirada)
     */
    public boolean isValid() {
        return active && !isRevoked() && !isExpired();
    }

    /**
     * Incrementa el contador de uso
     */
    public void incrementUsage() {
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Determina si esta es una key LIVE o TEST basándose en el prefijo
     * @return true si es LIVE (sk_live_*), false si es TEST (sk_test_*)
     */
    public boolean isLive() {
        if (keyPrefix == null) {
            return false; // Default a TEST si no hay prefijo
        }
        return keyPrefix.startsWith("sk_live_");
    }
}
