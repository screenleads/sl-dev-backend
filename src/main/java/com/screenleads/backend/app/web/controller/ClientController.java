package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.domain.model.Client;
import com.screenleads.backend.app.domain.repositories.ClientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/clients")
public class ClientController {
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

    public ClientController(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @GetMapping
    public ResponseEntity<List<Client>> listClients() {
        return ResponseEntity.ok(clientRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        client.setActive(true);
        return ResponseEntity.ok(clientRepository.save(client));
    }

    @PutMapping("/{id}")
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
