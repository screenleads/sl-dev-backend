package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface ApiKeyService {
    /**
     * Crea una nueva API key con hash seguro
     * @return Objeto con la API key Y la key en texto plano (solo visible una vez)
     */
    ApiKeyCreationResult createApiKey(String clientId, String scopes, int daysValid, boolean isLive);

    void deactivateApiKey(Long id);

    void activateApiKey(Long id);

    void deleteApiKey(Long id);

    ApiKeyCreationResult createApiKeyByDbId(Long clientDbId, String scopes, int daysValid, boolean isLive);

    List<ApiKey> getApiKeysByClientDbId(Long clientDbId);

    List<ApiKey> getAllApiKeys();

    Optional<ApiKey> getApiKeyById(Long id);

    ApiKey saveApiKey(ApiKey apiKey);

    ApiKey updateScopes(Long id, String scopes);

    ApiKey updateDescription(Long id, String description);

    ApiKey updateCompanyScope(Long id, Long companyScope);

    /**
     * Revoca una API key permanentemente
     * @param id ID de la key a revocar
     * @param reason Motivo de la revocación
     * @param revokedByUserId ID del usuario que revoca
     */
    ApiKey revokeApiKey(Long id, String reason, Long revokedByUserId);

    /**
     * Rota una API key (crea una nueva y marca la vieja como revocada)
     * @param id ID de la key a rotar
     * @param daysValid Días de validez para la nueva key
     * @return Resultado con la nueva key en texto plano
     */
    ApiKeyCreationResult rotateApiKey(Long id, int daysValid);

    /**
     * Valida una API key en texto plano contra su hash
     * @param rawApiKey Key en texto plano
     * @return Optional con la ApiKey si es válida
     */
    Optional<ApiKey> validateApiKey(String rawApiKey);

    /**
     * Registra el uso de una API key (incrementa contador y actualiza lastUsedAt)
     */
    void recordUsage(Long apiKeyId);

    /**
     * Clase para devolver la key creada (con texto plano)
     */
    class ApiKeyCreationResult {
        private final ApiKey apiKey;
        private final String rawKey; // Solo se devuelve al crear

        public ApiKeyCreationResult(ApiKey apiKey, String rawKey) {
            this.apiKey = apiKey;
            this.rawKey = rawKey;
        }

        public ApiKey getApiKey() {
            return apiKey;
        }

        public String getRawKey() {
            return rawKey;
        }
    }
}
