package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.ApiKeyService;
import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.web.dto.ApiKeyDTO;
import com.screenleads.backend.app.web.mapper.ApiKeyMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyDTO>> getAllApiKeys() {
        List<ApiKey> keys = apiKeyService.getAllApiKeys();
        return ResponseEntity.ok(keys.stream()
                .map(ApiKeyMapper::toDto)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyDTO> getApiKeyById(@PathVariable Long id) {
        return apiKeyService.getApiKeyById(id)
                .map(ApiKeyMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiKeyDTO> createApiKey(@RequestBody ApiKeyDTO apiKeyDTO) {
        if (apiKeyDTO.getClientId() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // Calculate days valid from expiresAt or use default
        int daysValid = 365;
        if (apiKeyDTO.getExpiresAt() != null) {
            daysValid = (int) java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDateTime.now(), 
                apiKeyDTO.getExpiresAt()
            );
        }
        
        ApiKey key = apiKeyService.createApiKeyByDbId(
            apiKeyDTO.getClientId(), 
            apiKeyDTO.getPermissions(), 
            daysValid
        );
        
        if (apiKeyDTO.getName() != null) {
            key.setName(apiKeyDTO.getName());
        }
        if (apiKeyDTO.getDescription() != null) {
            key.setDescription(apiKeyDTO.getDescription());
        }
        key = apiKeyService.saveApiKey(key);
        
        return ResponseEntity.ok(ApiKeyMapper.toDto(key));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiKeyDTO> updateApiKey(@PathVariable Long id, @RequestBody ApiKeyDTO apiKeyDTO) {
        return apiKeyService.getApiKeyById(id)
            .map(existingKey -> {
                if (apiKeyDTO.getName() != null) {
                    existingKey.setName(apiKeyDTO.getName());
                }
                if (apiKeyDTO.getPermissions() != null) {
                    existingKey.setPermissions(apiKeyDTO.getPermissions());
                }
                if (apiKeyDTO.getDescription() != null) {
                    existingKey.setDescription(apiKeyDTO.getDescription());
                }
                if (apiKeyDTO.getExpiresAt() != null) {
                    existingKey.setExpiresAt(apiKeyDTO.getExpiresAt());
                }
                existingKey.setActive(apiKeyDTO.isActive());
                
                ApiKey updated = apiKeyService.saveApiKey(existingKey);
                return ResponseEntity.ok(ApiKeyMapper.toDto(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateApiKey(@PathVariable Long id) {
        apiKeyService.activateApiKey(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateApiKey(@PathVariable Long id) {
        apiKeyService.deactivateApiKey(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/client/{clientDbId}")
    public ResponseEntity<List<ApiKeyDTO>> getApiKeysByClient(@PathVariable Long clientDbId) {
        List<ApiKey> keys = apiKeyService.getApiKeysByClientDbId(clientDbId);
        return ResponseEntity.ok(keys.stream()
                .map(ApiKeyMapper::toDto)
                .toList());
    }

    @PatchMapping("/{id}/permissions")
    public ResponseEntity<ApiKeyDTO> updatePermissions(@PathVariable Long id, @RequestParam String permissions) {
        ApiKey updated = apiKeyService.updatePermissions(id, permissions);
        return ResponseEntity.ok(ApiKeyMapper.toDto(updated));
    }

    @PatchMapping("/{id}/description")
    public ResponseEntity<ApiKeyDTO> updateDescription(@PathVariable Long id, @RequestParam String description) {
        ApiKey updated = apiKeyService.updateDescription(id, description);
        return ResponseEntity.ok(ApiKeyMapper.toDto(updated));
    }

    @PatchMapping("/{id}/company-scope")
    public ResponseEntity<ApiKeyDTO> updateCompanyScope(@PathVariable Long id,
            @RequestParam(required = false) Long companyScope) {
        ApiKey updated = apiKeyService.updateCompanyScope(id, companyScope);
        return ResponseEntity.ok(ApiKeyMapper.toDto(updated));
    }
}
