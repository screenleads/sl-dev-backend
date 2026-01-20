package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.domain.model.ApiClient;
import com.screenleads.backend.app.domain.repositories.ClientRepository;
import com.screenleads.backend.app.application.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
@CrossOrigin(origins = "*")
public class ClientController {
    private com.screenleads.backend.app.web.dto.ClientDTO toDTO(ApiClient client) {
        com.screenleads.backend.app.web.dto.ClientDTO dto = new com.screenleads.backend.app.web.dto.ClientDTO();
        dto.setId(client.getId());
        dto.setClientId(client.getClientId());
        dto.setName(client.getName());
        dto.setActive(client.getActive());
        if (client.getApiKeys() != null) {
            java.util.List<com.screenleads.backend.app.web.dto.ApiKeyDTO> apiKeyDTOs = client.getApiKeys().stream()
                    .map(apiKey -> {
                        com.screenleads.backend.app.web.dto.ApiKeyDTO akDto = new com.screenleads.backend.app.web.dto.ApiKeyDTO();
                        akDto.setId(apiKey.getId());
                        akDto.setKey(apiKey.getKey());
                        akDto.setActive(apiKey.isActive());
                        akDto.setCreatedAt(apiKey.getCreatedAt());
                        akDto.setExpiresAt(apiKey.getExpiresAt());
                        akDto.setPermissions(apiKey.getPermissions());
                        return akDto;
                    }).toList();
            dto.setApiKeys(apiKeyDTOs);
        }
        return dto;
    }

    private ResponseEntity<Void> activate(ApiClient client) {
        client.setActive(true);
        clientRepository.save(client);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Void> deactivate(ApiClient client) {
        client.setActive(false);
        clientRepository.save(client);
        return ResponseEntity.ok().build();
    }

    private final ClientRepository clientRepository;

    private final ApiKeyService apiKeyService;

    public ClientController(ClientRepository clientRepository, ApiKeyService apiKeyService) {
        this.clientRepository = clientRepository;
        this.apiKeyService = apiKeyService;
    }

    @PreAuthorize("@perm.can('client', 'read')")
    @GetMapping
    public ResponseEntity<java.util.List<com.screenleads.backend.app.web.dto.ClientDTO>> listClients() {
        java.util.List<ApiClient> clients = clientRepository.findAll();
        java.util.List<com.screenleads.backend.app.web.dto.ClientDTO> dtos = clients.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("@perm.can('client', 'read')")
    @GetMapping("/{id}")
    public ResponseEntity<com.screenleads.backend.app.web.dto.ClientDTO> getClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<com.screenleads.backend.app.web.dto.ClientDTO> createClient(@RequestBody ApiClient client) {
        // Autogenerar clientId alfanumérico
        client.setClientId(java.util.UUID.randomUUID().toString());
        client.setActive(true);
        ApiClient saved = clientRepository.save(client);
        // Crear la primera API Key con permisos básicos de lectura
        String defaultPermissions = "device:read,customer:read,promotion:read,advice:read,media:read";
        com.screenleads.backend.app.domain.model.ApiKey defaultApiKey = apiKeyService.createApiKeyByDbId(saved.getId(),
                defaultPermissions, 365);
        // Establecer nombre y descripción por defecto
        defaultApiKey.setName("API Key Principal - " + saved.getName());
        defaultApiKey.setDescription("API Key generada automáticamente al crear el cliente");
        apiKeyService.saveApiKey(defaultApiKey);
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<com.screenleads.backend.app.web.dto.ClientDTO> updateClient(@PathVariable Long id,
            @RequestBody ApiClient client) {
        return clientRepository.findById(id)
                .map(existing -> {
                    existing.setName(client.getName());
                    existing.setClientId(client.getClientId());
                    existing.setActive(client.getActive());
                    ApiClient saved = clientRepository.save(existing);
                    return ResponseEntity.ok(toDTO(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(this::activate)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(this::deactivate)
                .orElse(ResponseEntity.notFound().build());
    }
}
