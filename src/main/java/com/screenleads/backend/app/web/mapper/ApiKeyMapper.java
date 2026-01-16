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
        dto.setKey(apiKey.getKey());
        dto.setName(apiKey.getName());
        dto.setDescription(apiKey.getDescription());
        dto.setActive(apiKey.isActive());
        dto.setCreatedAt(apiKey.getCreatedAt());
        dto.setExpiresAt(apiKey.getExpiresAt());
        dto.setPermissions(apiKey.getPermissions());
        
        // Map clientId from the Client relationship
        if (apiKey.getApiClient() != null) {
            dto.setClientId(apiKey.getApiClient().getId());
        }

        return dto;
    }
}
