package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.ApiKeyService;
import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.web.dto.ApiKeyDTO;
import com.screenleads.backend.app.web.mapper.ApiKeyMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiKeyDTO> createApiKey(@RequestParam Long clientDbId,
            @RequestParam String permissions,
            @RequestParam(defaultValue = "365") int daysValid) {
        ApiKey key = apiKeyService.createApiKeyByDbId(clientDbId, permissions, daysValid);
        return ResponseEntity.ok(ApiKeyMapper.toDto(key));
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
                .toList()));
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
