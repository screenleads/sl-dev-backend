package com.screenleads.backend.app.application.security;

import java.util.Set;

/**
 * Representa el principal de autenticación para una API Key.
 * Contiene toda la información necesaria para verificar permisos y aplicar filtros de datos.
 */
public class ApiKeyPrincipal {
    private final Long apiKeyId;
    private final String clientId;
    private final Long clientDbId;
    private final Set<String> permissions;
    private final Long companyScope; // null = acceso a todas las compañías

    public ApiKeyPrincipal(Long apiKeyId, String clientId, Long clientDbId, Set<String> permissions, Long companyScope) {
        this.apiKeyId = apiKeyId;
        this.clientId = clientId;
        this.clientDbId = clientDbId;
        this.permissions = permissions;
        this.companyScope = companyScope;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public String getClientId() {
        return clientId;
    }

    public Long getClientDbId() {
        return clientDbId;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public Long getCompanyScope() {
        return companyScope;
    }

    /**
     * Verifica si tiene un permiso específico.
     * @param resource El recurso (ej: "snapshot", "lead", "company")
     * @param action La acción (ej: "read", "create", "update", "delete")
     * @return true si tiene el permiso
     */
    public boolean hasPermission(String resource, String action) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        // Formatos soportados:
        // - "snapshot:read" (permiso específico)
        // - "snapshot:*" (todas las acciones sobre snapshot)
        // - "*:read" (leer cualquier recurso)
        // - "*:*" o "*" (superadmin)
        
        String specific = resource + ":" + action;
        String allActions = resource + ":*";
        String allResources = "*:" + action;
        
        return permissions.contains(specific) 
            || permissions.contains(allActions)
            || permissions.contains(allResources)
            || permissions.contains("*:*")
            || permissions.contains("*");
    }

    /**
     * Indica si tiene acceso global a todas las compañías.
     * @return true si puede acceder a datos de todas las compañías sin filtro
     */
    public boolean hasGlobalAccess() {
        return companyScope == null;
    }

    /**
     * Indica si está restringido a una compañía específica.
     * @return true si solo puede acceder a datos de una compañía
     */
    public boolean hasRestrictedAccess() {
        return companyScope != null;
    }

    @Override
    public String toString() {
        return "ApiKeyPrincipal{" +
                "apiKeyId=" + apiKeyId +
                ", clientId='" + clientId + '\'' +
                ", clientDbId=" + clientDbId +
                ", permissions=" + permissions +
                ", companyScope=" + companyScope +
                '}';
    }
}
