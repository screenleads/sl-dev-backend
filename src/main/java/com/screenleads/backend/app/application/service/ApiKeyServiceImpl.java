package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApiKeyServiceImpl implements ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public ApiKey createApiKey(String clientId, String permissions, int daysValid) {
        ApiKey key = new ApiKey();
        key.setKey(UUID.randomUUID().toString().replace("-", ""));
        key.setClientId(clientId);
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
    public List<ApiKey> getApiKeysByClient(String clientId) {
        return apiKeyRepository.findAll().stream()
                .filter(key -> key.getClientId().equals(clientId))
                .toList();
    }
}
