package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.ApiKey;
import java.util.List;
import java.util.Optional;

public interface ApiKeyService {
    ApiKey createApiKey(String clientId, String permissions, int daysValid);

    void deactivateApiKey(Long id);

    void activateApiKey(Long id);

    void deleteApiKey(Long id);

    ApiKey createApiKeyByDbId(Long clientDbId, String permissions, int daysValid);

    List<ApiKey> getApiKeysByClientDbId(Long clientDbId);

    List<ApiKey> getAllApiKeys();

    Optional<ApiKey> getApiKeyById(Long id);

    ApiKey saveApiKey(ApiKey apiKey);

    ApiKey updatePermissions(Long id, String permissions);

    ApiKey updateDescription(Long id, String description);

    ApiKey updateCompanyScope(Long id, Long companyScope);
}
