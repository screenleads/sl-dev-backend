package com.screenleads.backend.app.web.dto;

import java.time.LocalDateTime;

public class ApiKeyDTO {
    private Long id;
    private String keyPrefix; // Solo mostramos el prefijo (sk_live_abc1...)
    private String name;
    private String description;
    private Long clientId;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private String scopes; // Reemplaza "permissions"
    private Long companyScope;
    private Integer usageCount;
    
    // Campos de revocación
    private LocalDateTime revokedAt;
    private String revokedReason;
    private Long revokedById;
    
    // Indicador de ambiente
    private Boolean isLive;

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getCompanyScope() {
        return companyScope;
    }

    public void setCompanyScope(Long companyScope) {
        this.companyScope = companyScope;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
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

    public Long getRevokedById() {
        return revokedById;
    }

    public void setRevokedById(Long revokedById) {
        this.revokedById = revokedById;
    }

    public Boolean isLive() {
        return isLive;
    }

    public void setLive(Boolean live) {
        isLive = live;
    }
}
