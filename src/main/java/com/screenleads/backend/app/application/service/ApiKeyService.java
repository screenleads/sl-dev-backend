package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.ApiKey;
import java.util.List;

public interface ApiKeyService {
    ApiKey createApiKey(String clientId, String permissions, int daysValid);

    void deactivateApiKey(Long id);

    void activateApiKey(Long id);

    void deleteApiKey(Long id);

    ApiKey createApiKeyByDbId(Long clientDbId, String permissions, int daysValid);

    List<ApiKey> getApiKeysByClientDbId(Long clientDbId);
}
