package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.ApiKeyService;
import com.screenleads.backend.app.domain.model.ApiKey;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('apikey', 'create')")
    public ResponseEntity<ApiKey> createApiKey(@RequestParam Long clientDbId,
            @RequestParam String permissions,
            @RequestParam(defaultValue = "365") int daysValid) {
        ApiKey key = apiKeyService.createApiKeyByDbId(clientDbId, permissions, daysValid);
        return ResponseEntity.ok(key);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('apikey', 'update')")
    public ResponseEntity<Void> activateApiKey(@PathVariable Long id) {
        apiKeyService.activateApiKey(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('apikey', 'update')")
    public ResponseEntity<Void> deactivateApiKey(@PathVariable Long id) {
        apiKeyService.deactivateApiKey(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('apikey', 'delete')")
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/client/{clientDbId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('apikey', 'read')")
    public ResponseEntity<List<ApiKey>> getApiKeysByClient(@PathVariable Long clientDbId) {
        List<ApiKey> keys = apiKeyService.getApiKeysByClientDbId(clientDbId);
        return ResponseEntity.ok(keys);
    }
}
