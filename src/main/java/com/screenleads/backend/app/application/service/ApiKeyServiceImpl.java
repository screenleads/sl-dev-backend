package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.application.util.ApiKeyGenerator;
import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.model.ApiClient;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.ClientRepository;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final String API_KEY_NOT_FOUND = "API Key no encontrada con id: ";

    private final ApiKeyRepository apiKeyRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository,
            ClientRepository clientRepository,
            UserRepository userRepository,
            ApiKeyGenerator apiKeyGenerator) {
        this.apiKeyRepository = apiKeyRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.apiKeyGenerator = apiKeyGenerator;
    }

    @Override
    public ApiKeyCreationResult createApiKey(String clientId, String scopes, int daysValid, boolean isLive) {
        ApiClient client = clientRepository.findByClientIdAndActiveTrue(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado o inactivo"));
        return createApiKeyInternal(client, scopes, daysValid, isLive);
    }

    @Override
    public ApiKeyCreationResult createApiKeyByDbId(Long clientDbId, String scopes, int daysValid, boolean isLive) {
        ApiClient client = clientRepository.findById(clientDbId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado por id"));
        return createApiKeyInternal(client, scopes, daysValid, isLive);
    }

    private ApiKeyCreationResult createApiKeyInternal(ApiClient client, String scopes, int daysValid, boolean isLive) {
        // Generar API key en texto plano
        String rawApiKey = apiKeyGenerator.generateApiKey(isLive);

        // Crear entidad ApiKey
        ApiKey key = new ApiKey();
        key.setKeyHash(apiKeyGenerator.hashApiKey(rawApiKey));
        key.setKeyPrefix(apiKeyGenerator.extractPrefix(rawApiKey));
        key.setApiClient(client);
        key.setScopes(scopes);
        key.setActive(true);
        key.setCreatedAt(LocalDateTime.now());
        key.setUsageCount(0);

        if (daysValid > 0) {
            key.setExpiresAt(LocalDateTime.now().plusDays(daysValid));
        }

        ApiKey saved = apiKeyRepository.save(key);

        // Devolver resultado con la key en texto plano (solo visible una vez)
        return new ApiKeyCreationResult(saved, rawApiKey);
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
    public List<ApiKey> getApiKeysByClientDbId(Long clientDbId) {
        return apiKeyRepository.findAllByApiClient_Id(clientDbId);
    }

    @Override
    public List<ApiKey> getAllApiKeys() {
        return apiKeyRepository.findAll();
    }

    @Override
    public Optional<ApiKey> getApiKeyById(Long id) {
        return apiKeyRepository.findById(id);
    }

    @Override
    public ApiKey saveApiKey(ApiKey apiKey) {
        return apiKeyRepository.save(apiKey);
    }

    @Override
    public ApiKey updateScopes(Long id, String scopes) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(API_KEY_NOT_FOUND + id));
        key.setScopes(scopes);
        return apiKeyRepository.save(key);
    }

    @Override
    public ApiKey updateDescription(Long id, String description) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(API_KEY_NOT_FOUND + id));
        key.setDescription(description);
        return apiKeyRepository.save(key);
    }

    @Override
    public ApiKey updateCompanyScope(Long id, Long companyScope) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(API_KEY_NOT_FOUND + id));
        key.setCompanyScope(companyScope);
        return apiKeyRepository.save(key);
    }

    @Override
    public ApiKey revokeApiKey(Long id, String reason, Long revokedByUserId) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(API_KEY_NOT_FOUND + id));

        if (key.isRevoked()) {
            throw new IllegalStateException("La API key ya está revocada");
        }

        key.setRevokedAt(LocalDateTime.now());
        key.setRevokedReason(reason);
        key.setActive(false);

        if (revokedByUserId != null) {
            User revokedBy = userRepository.findById(revokedByUserId).orElse(null);
            key.setRevokedBy(revokedBy);
        }

        return apiKeyRepository.save(key);
    }

    @Override
    public ApiKeyCreationResult rotateApiKey(Long id, int daysValid) {
        ApiKey oldKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(API_KEY_NOT_FOUND + id));

        // Revocar la key antigua
        oldKey.setRevokedAt(LocalDateTime.now());
        oldKey.setRevokedReason("Rotada - reemplazada por nueva key");
        oldKey.setActive(false);
        apiKeyRepository.save(oldKey);

        // Crear nueva key con los mismos scopes y companyScope
        boolean isLive = apiKeyGenerator.isLiveKey(oldKey.getKeyPrefix() + "dummy");
        ApiKeyCreationResult newKey = createApiKeyInternal(
                oldKey.getApiClient(),
                oldKey.getScopes(),
                daysValid,
                isLive);

        // Copiar metadatos
        newKey.getApiKey().setName(oldKey.getName());
        newKey.getApiKey().setDescription(oldKey.getDescription());
        newKey.getApiKey().setCompanyScope(oldKey.getCompanyScope());
        apiKeyRepository.save(newKey.getApiKey());

        return newKey;
    }

    @Override
    public Optional<ApiKey> validateApiKey(String rawApiKey) {
        if (!apiKeyGenerator.isValidFormat(rawApiKey)) {
            return Optional.empty();
        }

        String prefix = apiKeyGenerator.extractPrefix(rawApiKey);

        // Buscar todas las keys con ese prefijo (debería ser solo una)
        List<ApiKey> candidateKeys = apiKeyRepository.findAll().stream()
                .filter(k -> prefix.equals(k.getKeyPrefix()))
                .toList();

        for (ApiKey key : candidateKeys) {
            if (apiKeyGenerator.matchesHash(rawApiKey, key.getKeyHash())) {
                // Verificar que la key sea válida
                if (key.isValid()) {
                    return Optional.of(key);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public void recordUsage(Long apiKeyId) {
        apiKeyRepository.findById(apiKeyId).ifPresent(key -> {
            key.incrementUsage();
            apiKeyRepository.save(key);
        });
    }
}
