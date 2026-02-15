package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.ApiKeyService;
import com.screenleads.backend.app.application.service.ApiKeyService.ApiKeyCreationResult;
import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.web.dto.ApiKeyDTO;
import com.screenleads.backend.app.web.mapper.ApiKeyMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> createApiKey(@RequestBody ApiKeyDTO apiKeyDTO) {
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
        
        // Determinar si es live o test (por defecto live)
        boolean isLive = apiKeyDTO.isLive() != null ? apiKeyDTO.isLive() : true;
        
        ApiKeyCreationResult result = apiKeyService.createApiKeyByDbId(
            apiKeyDTO.getClientId(), 
            apiKeyDTO.getScopes(), 
            daysValid,
            isLive
        );
        
        if (apiKeyDTO.getName() != null) {
            result.getApiKey().setName(apiKeyDTO.getName());
        }
        if (apiKeyDTO.getDescription() != null) {
            result.getApiKey().setDescription(apiKeyDTO.getDescription());
        }
        ApiKey saved = apiKeyService.saveApiKey(result.getApiKey());
        
        // IMPORTANTE: Devolver la key en texto plano SOLO al crear
        return ResponseEntity.ok(Map.of(
            "apiKey", ApiKeyMapper.toDto(saved),
            "rawKey", result.getRawKey() // Solo visible una vez
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiKeyDTO> updateApiKey(@PathVariable Long id, @RequestBody ApiKeyDTO apiKeyDTO) {
        return apiKeyService.getApiKeyById(id)
            .map(existingKey -> {
                if (apiKeyDTO.getName() != null) {
                    existingKey.setName(apiKeyDTO.getName());
                }
                if (apiKeyDTO.getScopes() != null) {
                    existingKey.setScopes(apiKeyDTO.getScopes());
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

    @PatchMapping("/{id}/scopes")
    public ResponseEntity<ApiKeyDTO> updateScopes(@PathVariable Long id, @RequestParam String scopes) {
        ApiKey updated = apiKeyService.updateScopes(id, scopes);
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

    /**
     * Revoca permanentemente una API key
     */
    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiKeyDTO> revokeApiKey(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        
        String reason = body.getOrDefault("reason", "No especificado");
        
        // Obtener ID del usuario autenticado (si existe)
        Long userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            // Extraer ID del usuario si está disponible
            // Ajustar según tu implementación de UserDetails
        }
        
        ApiKey revoked = apiKeyService.revokeApiKey(id, reason, userId);
        return ResponseEntity.ok(ApiKeyMapper.toDto(revoked));
    }

    /**
     * Rota una API key (crea nueva y revoca la vieja)
     */
    @PostMapping("/{id}/rotate")
    public ResponseEntity<Map<String, Object>> rotateApiKey(@PathVariable Long id) {
        ApiKeyCreationResult result = apiKeyService.rotateApiKey(id, 365); // 1 año default
        
        return ResponseEntity.ok(Map.of(
            "apiKey", ApiKeyMapper.toDto(result.getApiKey()),
            "rawKey", result.getRawKey() // Nueva key en texto plano
        ));
    }
}
