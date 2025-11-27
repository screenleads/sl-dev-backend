package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.ApiKeyService;
import com.screenleads.backend.app.domain.model.ApiKey;
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

    @PostMapping
    public ResponseEntity<ApiKey> createApiKey(@RequestParam String clientId,
                                               @RequestParam String permissions,
                                               @RequestParam(defaultValue = "365") int daysValid) {
        ApiKey key = apiKeyService.createApiKey(clientId, permissions, daysValid);
        return ResponseEntity.ok(key);
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

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ApiKey>> getApiKeysByClient(@PathVariable String clientId) {
        List<ApiKey> keys = apiKeyService.getApiKeysByClient(clientId);
        return ResponseEntity.ok(keys);
    }
}
