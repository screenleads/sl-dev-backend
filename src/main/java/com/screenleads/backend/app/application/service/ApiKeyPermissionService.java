package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("apiKeyPerm")
@Slf4j
public class ApiKeyPermissionService {
    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyPermissionService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public boolean can(String resource, String action) {
        log.info("🔑 ApiKeyPermissionService.can({}, {})", resource, action);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("❌ No hay autenticación o no está autenticado");
            return false;
        }

        String authority = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse(null);
        log.info("Authority: {}", authority);
        if (!"API_CLIENT".equals(authority)) {
            log.warn("❌ Authority no es API_CLIENT: {}", authority);
            return false;
        }

        Object principal = auth.getPrincipal();
        log.info("Principal: {} (type: {})", principal, principal != null ? principal.getClass().getName() : "null");

        Long apiKeyId = null;
        if (principal instanceof Long id) {
            apiKeyId = id;
        } else if (principal instanceof String str) {
            try {
                apiKeyId = Long.valueOf(str);
            } catch (NumberFormatException e) {
                log.error("❌ Error convirtiendo principal a Long: {}", principal, e);
                return false;
            }
        }

        if (apiKeyId == null) {
            log.warn("❌ apiKeyId es null");
            return false;
        }

        log.info("Buscando API key por ID: {}", apiKeyId);
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .filter(ApiKey::isActive)
                .orElse(null);

        if (apiKey == null) {
            log.warn("❌ No se encontró API key activa con ID: {}", apiKeyId);
            return false;
        }

        log.info("✅ API key encontrada - ID: {}, Scopes: {}", apiKey.getId(), apiKey.getScopes());
        String requiredPermission = resource + ":" + action;
        boolean hasPermission = apiKey.getScopes() != null && apiKey.getScopes().contains(requiredPermission);
        log.info("Buscando permiso '{}' en '{}': {}", requiredPermission, apiKey.getScopes(), hasPermission);

        return hasPermission;
    }

    /**
     * Obtiene el scope de compañía de la API Key autenticada.
     * Retorna null si tiene acceso global o si no está autenticada.
     */
    public Long getCompanyScope() {
        ApiKey apiKey = getAuthenticatedApiKey();
        if (apiKey == null)
            return null;
        return apiKey.getCompanyScope();
    }

    /**
     * Verifica si la API Key autenticada tiene acceso global (sin restricción de
     * compañía).
     */
    public boolean hasGlobalAccess() {
        ApiKey apiKey = getAuthenticatedApiKey();
        if (apiKey == null)
            return false;
        return apiKey.getCompanyScope() == null;
    }

    /**
     * Verifica si la API Key puede acceder a datos de una compañía específica.
     */
    public boolean canAccessCompany(Long companyId) {
        if (companyId == null)
            return false;

        ApiKey apiKey = getAuthenticatedApiKey();
        if (apiKey == null)
            return false;

        // Si tiene acceso global, puede acceder a cualquier compañía
        if (apiKey.getCompanyScope() == null)
            return true;

        // Si tiene scope de compañía, solo puede acceder a esa compañía
        return companyId.equals(apiKey.getCompanyScope());
    }

    /**
     * Método auxiliar para obtener la API Key autenticada del contexto de
     * seguridad.
     */
    private ApiKey getAuthenticatedApiKey() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;

        if (!"API_CLIENT".equals(auth.getAuthorities().stream().findFirst().map(Object::toString).orElse(null)))
            return null;

        Object principal = auth.getPrincipal();
        Long apiKeyId = null;

        if (principal instanceof Long id) {
            apiKeyId = id;
        } else if (principal instanceof String str) {
            try {
                apiKeyId = Long.valueOf(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (apiKeyId == null)
            return null;

        return apiKeyRepository.findById(apiKeyId)
                .filter(ApiKey::isActive)
                .orElse(null);
    }
}
