package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.model.Client;
import com.screenleads.backend.app.domain.repositories.ClientRepository;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final String API_KEY_NOT_FOUND = "API Key no encontrada con id: ";

    private final ApiKeyRepository apiKeyRepository;
    private final ClientRepository clientRepository;

    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository, ClientRepository clientRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public ApiKey createApiKey(String clientId, String permissions, int daysValid) {
        Client client = clientRepository.findByClientIdAndActiveTrue(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado o inactivo"));
        ApiKey key = new ApiKey();
        key.setKey(UUID.randomUUID().toString().replace("-", ""));
        key.setClient(client);
        key.setPermissions(permissions);
        key.setActive(true);
        key.setCreatedAt(LocalDateTime.now());
        key.setExpiresAt(LocalDateTime.now().plusDays(daysValid));
        return apiKeyRepository.save(key);
    }

    @Override
    public void deactivateApiKey(Long id) {
        apiKeyRepository.findById(id).ifPresent(key -> {
            key.setActive(false);
            apiKeyRepository.save(key);
        });
    }

    @Override
    public void activateApiKey(Long id) {
        apiKeyRepository.findById(id).ifPresent(key -> {
            key.setActive(true);
            apiKeyRepository.save(key);
        });
    }

    @Override
    public void deleteApiKey(Long id) {
        apiKeyRepository.deleteById(id);
    }

    @Override
    public ApiKey createApiKeyByDbId(Long clientDbId, String permissions, int daysValid) {
        Client client = clientRepository.findById(clientDbId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado por id"));
        ApiKey key = new ApiKey();
        key.setKey(UUID.randomUUID().toString().replace("-", ""));
        key.setClient(client);
        key.setPermissions(permissions);
        key.setActive(true);
        key.setCreatedAt(LocalDateTime.now());
        key.setExpiresAt(LocalDateTime.now().plusDays(daysValid));
        return apiKeyRepository.save(key);
    }

    @Override
    public List<ApiKey> getApiKeysByClientDbId(Long clientDbId) {
        // Usar el método del repositorio para evitar errores de tipo
        return apiKeyRepository.findAllByClient_Id(clientDbId);
    }

    @Override
    public ApiKey updatePermissions(Long id, String permissions) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key no encontrada con id: " + id));
        key.setPermissions(permissions);
        return apiKeyRepository.save(key);
    }

    @Override
    public ApiKey updateDescription(Long id, String description) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key no encontrada con id: " + id));
        key.setDescription(description);
        return apiKeyRepository.save(key);
    }

    @Override
    public ApiKey updateCompanyScope(Long id, Long companyScope) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key no encontrada con id: " + id));
        key.setCompanyScope(companyScope);
        return apiKeyRepository.save(key);
    }
}
