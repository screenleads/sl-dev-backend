package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("apiKeyPerm")
public class ApiKeyPermissionService {
    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyPermissionService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public boolean can(String resource, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return false;
        if (!"API_CLIENT".equals(auth.getAuthorities().stream().findFirst().map(Object::toString).orElse(null)))
            return false;
        Object principal = auth.getPrincipal();
        Long clientDbId = null;
        if (principal instanceof Long) {
            clientDbId = (Long) principal;
        } else if (principal instanceof String) {
            try {
                clientDbId = Long.valueOf((String) principal);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (clientDbId == null)
            return false;
        ApiKey apiKey = apiKeyRepository.findAllByClient_Id(clientDbId).stream()
                .filter(ApiKey::isActive)
                .findFirst().orElse(null);
        if (apiKey == null)
            return false;
        // Permisos: puedes usar lógica más avanzada si lo necesitas
        return apiKey.getPermissions() != null && apiKey.getPermissions().contains(resource + ":" + action);
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
     * Verifica si la API Key autenticada tiene acceso global (sin restricción de compañía).
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
     * Método auxiliar para obtener la API Key autenticada del contexto de seguridad.
     */
    private ApiKey getAuthenticatedApiKey() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        
        if (!"API_CLIENT".equals(auth.getAuthorities().stream().findFirst().map(Object::toString).orElse(null)))
            return null;
        
        Object principal = auth.getPrincipal();
        Long clientDbId = null;
        
        if (principal instanceof Long) {
            clientDbId = (Long) principal;
        } else if (principal instanceof String) {
            try {
                clientDbId = Long.valueOf((String) principal);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        if (clientDbId == null)
            return null;
        
        return apiKeyRepository.findAllByClient_Id(clientDbId).stream()
                .filter(ApiKey::isActive)
                .findFirst().orElse(null);
    }
}
