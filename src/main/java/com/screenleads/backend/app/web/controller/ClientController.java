package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.domain.model.Client;
import com.screenleads.backend.app.domain.repositories.ClientRepository;
import com.screenleads.backend.app.application.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/clients")
public class ClientController {
        private com.screenleads.backend.app.web.dto.ClientDTO toDTO(Client client) {
            com.screenleads.backend.app.web.dto.ClientDTO dto = new com.screenleads.backend.app.web.dto.ClientDTO();
            dto.setId(client.getId());
            dto.setClientId(client.getClientId());
            dto.setName(client.getName());
            dto.setActive(client.isActive());
            if (client.getApiKeys() != null) {
                java.util.List<com.screenleads.backend.app.web.dto.ApiKeyDTO> apiKeyDTOs = client.getApiKeys().stream().map(apiKey -> {
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
    private ResponseEntity<Void> activate(Client client) {
        client.setActive(true);
        clientRepository.save(client);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Void> deactivate(Client client) {
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

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('client', 'read')")
    public ResponseEntity<java.util.List<com.screenleads.backend.app.web.dto.ClientDTO>> listClients() {
        java.util.List<Client> clients = clientRepository.findAll();
        java.util.List<com.screenleads.backend.app.web.dto.ClientDTO> dtos = clients.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('client', 'read')")
    public ResponseEntity<Client> getClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('client', 'create')")
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        // Autogenerar clientId alfanumérico
        client.setClientId(java.util.UUID.randomUUID().toString());
        client.setActive(true);
        Client saved = clientRepository.save(client);
        // Crear la primera API Key relacionada usando el id interno
        apiKeyService.createApiKeyByDbId(saved.getId(), "DEFAULT", 365);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('client', 'update')")
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @RequestBody Client client) {
        return clientRepository.findById(id)
                .map(existing -> {
                    existing.setName(client.getName());
                    existing.setClientId(client.getClientId());
                    existing.setActive(client.isActive());
                    return ResponseEntity.ok(clientRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('client', 'delete')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('client', 'update')")
    public ResponseEntity<Void> activateClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(this::activate)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('client', 'update')")
    public ResponseEntity<Void> deactivateClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(this::deactivate)
                .orElse(ResponseEntity.notFound().build());
    }
}
