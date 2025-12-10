package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.web.dto.ApiKeyDTO;

public class ApiKeyMapper {
    
    public static ApiKeyDTO toDto(ApiKey apiKey) {
        if (apiKey == null) {
            return null;
        }
        
        ApiKeyDTO dto = new ApiKeyDTO();
        dto.setId(apiKey.getId());
        dto.setKey(apiKey.getKey());
        dto.setActive(apiKey.isActive());
        dto.setCreatedAt(apiKey.getCreatedAt());
        dto.setExpiresAt(apiKey.getExpiresAt());
        dto.setPermissions(apiKey.getPermissions());
        
        return dto;
    }
}
