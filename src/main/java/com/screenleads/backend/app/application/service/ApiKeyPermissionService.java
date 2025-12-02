package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.application.security.ApiKeyPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Servicio para verificar permisos de API Keys.
 * Las API Keys tienen permisos granulares en formato "resource:action"
 * y pueden tener alcance global o restringido a una compañía específica.
 */
@Slf4j
@Service("apiKeyPerm")
public class ApiKeyPermissionService {

    /**
     * Verifica si la API Key autenticada tiene permiso para realizar una acción sobre un recurso.
     * 
     * @param resource El recurso (ej: "snapshot", "lead", "company")
     * @param action La acción (ej: "read", "create", "update", "delete")
     * @return true si tiene el permiso
     */
    public boolean can(String resource, String action) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                log.debug("No hay autenticación o no está autenticado");
                return false;
            }

            // Verificar que es una autenticación de API_CLIENT
            boolean isApiClient = auth.getAuthorities().stream()
                .anyMatch(a -> "API_CLIENT".equals(a.getAuthority()));
            
            if (!isApiClient) {
                log.debug("No es una autenticación de API_CLIENT");
                return false;
            }

            Object principal = auth.getPrincipal();
            if (!(principal instanceof ApiKeyPrincipal)) {
                log.warn("Principal no es ApiKeyPrincipal: {}", principal.getClass().getName());
                return false;
            }

            ApiKeyPrincipal apiKeyPrincipal = (ApiKeyPrincipal) principal;
            boolean hasPermission = apiKeyPrincipal.hasPermission(resource, action);
            
            log.debug("API Key {} verificando permiso {}:{} = {}", 
                apiKeyPrincipal.getClientId(), resource, action, hasPermission);
            
            return hasPermission;
            
        } catch (Exception e) {
            log.error("Error verificando permisos de API Key para {}:{}", resource, action, e);
            return false;
        }
    }

    /**
     * Obtiene el ID de la compañía a la que está restringida la API Key.
     * @return ID de compañía o null si tiene acceso global
     */
    public Long getCompanyScope() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof ApiKeyPrincipal)) {
                return null;
            }
            
            ApiKeyPrincipal principal = (ApiKeyPrincipal) auth.getPrincipal();
            return principal.getCompanyScope();
            
        } catch (Exception e) {
            log.error("Error obteniendo company scope", e);
            return null;
        }
    }

    /**
     * Verifica si la API Key tiene acceso global (todas las compañías).
     * @return true si puede acceder a datos de todas las compañías
     */
    public boolean hasGlobalAccess() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof ApiKeyPrincipal)) {
                return false;
            }
            
            ApiKeyPrincipal principal = (ApiKeyPrincipal) auth.getPrincipal();
            return principal.hasGlobalAccess();
            
        } catch (Exception e) {
            log.error("Error verificando acceso global", e);
            return false;
        }
    }

    /**
     * Verifica si la API Key puede acceder a datos de una compañía específica.
     * @param companyId ID de la compañía
     * @return true si puede acceder a datos de esa compañía
     */
    public boolean canAccessCompany(Long companyId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof ApiKeyPrincipal)) {
                return false;
            }
            
            ApiKeyPrincipal principal = (ApiKeyPrincipal) auth.getPrincipal();
            
            // Si tiene acceso global, puede acceder a cualquier compañía
            if (principal.hasGlobalAccess()) {
                return true;
            }
            
            // Si está restringido, solo puede acceder a su compañía asignada
            return principal.getCompanyScope() != null 
                && principal.getCompanyScope().equals(companyId);
            
        } catch (Exception e) {
            log.error("Error verificando acceso a compañía {}", companyId, e);
            return false;
        }
    }
}
