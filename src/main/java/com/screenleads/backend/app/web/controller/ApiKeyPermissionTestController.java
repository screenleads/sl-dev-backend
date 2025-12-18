package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.ApiKeyPermissionService;
import com.screenleads.backend.app.application.security.ApiKeyPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Ejemplo de controlador que demuestra el uso del sistema de permisos de API Keys.
 * Este controlador puede ser usado para testing y como referencia.
 */
@RestController
@RequestMapping("/api/test-permissions")
public class ApiKeyPermissionTestController {

    private final ApiKeyPermissionService apiKeyPermissionService;

    public ApiKeyPermissionTestController(ApiKeyPermissionService apiKeyPermissionService) {
        this.apiKeyPermissionService = apiKeyPermissionService;
    }

    /**
     * Endpoint público que muestra información sobre la API Key autenticada.
     * Útil para debugging y verificación.
     */
    @GetMapping("/info")
    public Map<String, Object> getApiKeyInfo() {
        Map<String, Object> info = new HashMap<>();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            info.put("authenticated", false);
            return info;
        }
        
        info.put("authenticated", true);
        info.put("principal_type", auth.getPrincipal().getClass().getSimpleName());
        
        if (auth.getPrincipal() instanceof ApiKeyPrincipal principal) {
            info.put("client_id", principal.getClientId());
            info.put("permissions", principal.getPermissions());
            info.put("company_scope", principal.getCompanyScope());
            info.put("has_global_access", principal.hasGlobalAccess());
            info.put("has_restricted_access", principal.hasRestrictedAccess());
            
            // Ejemplos de verificación de permisos
            Map<String, Boolean> permissionChecks = new HashMap<>();
            permissionChecks.put("snapshot:read", principal.hasPermission("snapshot", "read"));
            permissionChecks.put("snapshot:create", principal.hasPermission("snapshot", "create"));
            permissionChecks.put("snapshot:update", principal.hasPermission("snapshot", "update"));
            permissionChecks.put("snapshot:delete", principal.hasPermission("snapshot", "delete"));
            permissionChecks.put("lead:read", principal.hasPermission("lead", "read"));
            permissionChecks.put("lead:create", principal.hasPermission("lead", "create"));
            
            info.put("permission_checks", permissionChecks);
        }
        
        return info;
    }

    /**
     * Endpoint que requiere permiso de lectura sobre snapshots.
     */
    @PreAuthorize("@apiKeyPerm.can('snapshot', 'read')")
    @GetMapping("/snapshot/read")
    public Map<String, Object> testSnapshotRead() {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "snapshot:read");
        result.put("success", true);
        result.put("message", "You have permission to read snapshots");
        result.put("company_scope", apiKeyPermissionService.getCompanyScope());
        return result;
    }

    /**
     * Endpoint que requiere permiso de creación sobre snapshots.
     */
    @PreAuthorize("@apiKeyPerm.can('snapshot', 'create')")
    @PostMapping("/snapshot/create")
    public Map<String, Object> testSnapshotCreate() {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "snapshot:create");
        result.put("success", true);
        result.put("message", "You have permission to create snapshots");
        result.put("company_scope", apiKeyPermissionService.getCompanyScope());
        return result;
    }

    /**
     * Endpoint que requiere permiso de actualización sobre leads.
     */
    @PreAuthorize("@apiKeyPerm.can('lead', 'update')")
    @PutMapping("/lead/update")
    public Map<String, Object> testLeadUpdate() {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "lead:update");
        result.put("success", true);
        result.put("message", "You have permission to update leads");
        result.put("company_scope", apiKeyPermissionService.getCompanyScope());
        return result;
    }

    /**
     * Endpoint que requiere permiso de eliminación sobre leads.
     */
    @PreAuthorize("@apiKeyPerm.can('lead', 'delete')")
    @DeleteMapping("/lead/delete")
    public Map<String, Object> testLeadDelete() {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "lead:delete");
        result.put("success", true);
        result.put("message", "You have permission to delete leads");
        result.put("company_scope", apiKeyPermissionService.getCompanyScope());
        return result;
    }

    /**
     * Endpoint que requiere acceso global (sin restricción de compañía).
     */
    @GetMapping("/global-access-required")
    public Map<String, Object> testGlobalAccess() {
        Map<String, Object> result = new HashMap<>();
        
        if (!apiKeyPermissionService.hasGlobalAccess()) {
            result.put("success", false);
            result.put("message", "This endpoint requires global access");
            result.put("your_scope", apiKeyPermissionService.getCompanyScope());
            return result;
        }
        
        result.put("success", true);
        result.put("message", "You have global access");
        return result;
    }

    /**
     * Endpoint que verifica si puede acceder a una compañía específica.
     */
    @GetMapping("/can-access-company/{companyId}")
    public Map<String, Object> testCompanyAccess(@PathVariable Long companyId) {
        Map<String, Object> result = new HashMap<>();
        result.put("company_id_requested", companyId);
        result.put("your_company_scope", apiKeyPermissionService.getCompanyScope());
        result.put("has_global_access", apiKeyPermissionService.hasGlobalAccess());
        
        boolean canAccess = apiKeyPermissionService.canAccessCompany(companyId);
        result.put("can_access", canAccess);
        
        if (canAccess) {
            result.put("message", "You can access data from company " + companyId);
        } else {
            result.put("message", "You cannot access data from company " + companyId);
        }
        
        return result;
    }

    /**
     * Endpoint de prueba múltiple que verifica varios permisos a la vez.
     */
    @GetMapping("/check-multiple")
    public Map<String, Object> checkMultiplePermissions(
            @RequestParam(required = false, defaultValue = "snapshot") String resource,
            @RequestParam(required = false, defaultValue = "read") String action) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("resource", resource);
        result.put("action", action);
        result.put("has_permission", apiKeyPermissionService.can(resource, action));
        result.put("company_scope", apiKeyPermissionService.getCompanyScope());
        result.put("has_global_access", apiKeyPermissionService.hasGlobalAccess());
        
        return result;
    }
}
