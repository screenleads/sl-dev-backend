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
 * Ejemplo de controlador que demuestra el uso del sistema de permisos de API
 * Keys.
 * Este controlador puede ser usado para testing y como referencia.
 */
@RestController
@RequestMapping("/api/test-permissions")
public class ApiKeyPermissionTestController {

    private static final String COMPANY_SCOPE = "company_scope";
    private static final String HAS_GLOBAL_ACCESS = "has_global_access";
    private static final String SNAPSHOT = "snapshot";
    private static final String ACTION = "action";
    private static final String SUCCESS = "success";
    private static final String MESSAGE = "message";

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
            info.put(COMPANY_SCOPE, principal.getCompanyScope());
            info.put(HAS_GLOBAL_ACCESS, principal.hasGlobalAccess());
            info.put("has_restricted_access", principal.hasRestrictedAccess());

            // Ejemplos de verificación de permisos
            Map<String, Boolean> permissionChecks = new HashMap<>();
            permissionChecks.put("snapshot:read", principal.hasPermission(SNAPSHOT, "read"));
            permissionChecks.put("snapshot:create", principal.hasPermission(SNAPSHOT, "create"));
            permissionChecks.put("snapshot:update", principal.hasPermission(SNAPSHOT, "update"));
            permissionChecks.put("snapshot:delete", principal.hasPermission(SNAPSHOT, "delete"));
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
        result.put(ACTION, "snapshot:read");
        result.put(SUCCESS, true);
        result.put(MESSAGE, "You have permission to read snapshots");
        result.put(COMPANY_SCOPE, apiKeyPermissionService.getCompanyScope());
        return result;
    }

    /**
     * Endpoint que requiere permiso de creación sobre snapshots.
     */
    @PreAuthorize("@apiKeyPerm.can('snapshot', 'create')")
    @PostMapping("/snapshot/create")
    public Map<String, Object> testSnapshotCreate() {
        Map<String, Object> result = new HashMap<>();
        result.put(ACTION, "snapshot:create");
        result.put(SUCCESS, true);
        result.put(MESSAGE, "You have permission to create snapshots");
        result.put(COMPANY_SCOPE, apiKeyPermissionService.getCompanyScope());
        return result;
    }

    /**
     * Endpoint que requiere permiso de actualización sobre leads.
     */
    @PreAuthorize("@apiKeyPerm.can('lead', 'update')")
    @PutMapping("/lead/update")
    public Map<String, Object> testLeadUpdate() {
        Map<String, Object> result = new HashMap<>();
        result.put(ACTION, "lead:update");
        result.put(SUCCESS, true);
        result.put(MESSAGE, "You have permission to update leads");
        result.put(COMPANY_SCOPE, apiKeyPermissionService.getCompanyScope());
        return result;
    }

    /**
     * Endpoint que requiere permiso de eliminación sobre leads.
     */
    @PreAuthorize("@apiKeyPerm.can('lead', 'delete')")
    @DeleteMapping("/lead/delete")
    public Map<String, Object> testLeadDelete() {
        Map<String, Object> result = new HashMap<>();
        result.put(ACTION, "lead:delete");
        result.put(SUCCESS, true);
        result.put(MESSAGE, "You have permission to delete leads");
        result.put(COMPANY_SCOPE, apiKeyPermissionService.getCompanyScope());
        return result;
    }

    /**
     * Endpoint que requiere acceso global (sin restricción de compañía).
     */
    @GetMapping("/global-access-required")
    public Map<String, Object> testGlobalAccess() {
        Map<String, Object> result = new HashMap<>();

        if (!apiKeyPermissionService.hasGlobalAccess()) {
            result.put(SUCCESS, false);
            result.put(MESSAGE, "This endpoint requires global access");
            result.put("your_scope", apiKeyPermissionService.getCompanyScope());
            return result;
        }

        result.put(SUCCESS, true);
        result.put(MESSAGE, "You have global access");
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
        result.put(HAS_GLOBAL_ACCESS, apiKeyPermissionService.hasGlobalAccess());

        boolean canAccess = apiKeyPermissionService.canAccessCompany(companyId);
        result.put("can_access", canAccess);

        if (canAccess) {
            result.put(MESSAGE, "You can access data from company " + companyId);
        } else {
            result.put(MESSAGE, "You cannot access data from company " + companyId);
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
        result.put(ACTION, action);
        result.put("has_permission", apiKeyPermissionService.can(resource, action));
        result.put(COMPANY_SCOPE, apiKeyPermissionService.getCompanyScope());
        result.put(HAS_GLOBAL_ACCESS, apiKeyPermissionService.hasGlobalAccess());

        return result;
    }
}
