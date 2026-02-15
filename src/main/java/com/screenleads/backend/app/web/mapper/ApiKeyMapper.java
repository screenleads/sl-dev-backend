package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.web.dto.ApiKeyDTO;

public class ApiKeyMapper {

    private ApiKeyMapper() {
    }

    public static ApiKeyDTO toDto(ApiKey apiKey) {
        if (apiKey == null) {
            return null;
        }

        ApiKeyDTO dto = new ApiKeyDTO();
        dto.setId(apiKey.getId());
        dto.setKeyPrefix(apiKey.getKeyPrefix()); // Solo el prefijo, no el hash
        dto.setName(apiKey.getName());
        dto.setDescription(apiKey.getDescription());
        dto.setActive(apiKey.isActive());
        dto.setCreatedAt(apiKey.getCreatedAt());
        dto.setExpiresAt(apiKey.getExpiresAt());
        dto.setLastUsedAt(apiKey.getLastUsedAt());
        dto.setScopes(apiKey.getScopes());
        dto.setCompanyScope(apiKey.getCompanyScope());
        dto.setUsageCount(apiKey.getUsageCount());
        
        // Campos de revocación
        dto.setRevokedAt(apiKey.getRevokedAt());
        dto.setRevokedReason(apiKey.getRevokedReason());
        if (apiKey.getRevokedBy() != null) {
            dto.setRevokedById(apiKey.getRevokedBy().getId());
        }
        
        // Determinar si es live o test por el prefijo
        dto.setLive(apiKey.getKeyPrefix() != null && apiKey.getKeyPrefix().startsWith("sk_live_"));

        // Map clientId from the Client relationship
        if (apiKey.getApiClient() != null) {
            dto.setClientId(apiKey.getApiClient().getId());
        }

        return dto;
    }
}
