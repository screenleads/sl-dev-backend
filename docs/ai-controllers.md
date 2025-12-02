# Controladores — snapshot incrustado

> REST controllers.

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/web/controller/AdvicesController.java
// src/main/java/com/screenleads/backend/app/web/controller/AdvicesController.java
package com.screenleads.backend.app.web.controller;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.AdviceService;
import com.screenleads.backend.app.web.dto.AdviceDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/advices")
@Tag(name = "Advices", description = "Gestión y consulta de anuncios (advices)")
public class AdvicesController {

    private static final Logger logger = LoggerFactory.getLogger(AdvicesController.class);
    private final AdviceService adviceService;

    public AdvicesController(AdviceService adviceService) {
        this.adviceService = adviceService;
    }

    @PreAuthorize("@perm.can('advice', 'read')")
    @GetMapping
    @Operation(summary = "Listar todos los advices")
    public ResponseEntity<List<AdviceDTO>> getAllAdvices() {
        return ResponseEntity.ok(adviceService.getAllAdvices());
    }

    /**
     * Devuelve los anuncios visibles "ahora" según la zona horaria del cliente,
     * leída de los headers:
     * - X-Timezone: IANA TZ (p.ej. "Europe/Madrid")
     * - X-Timezone-Offset: minutos al ESTE de UTC (p.ej. "120")
     */
    @PreAuthorize("@perm.can('advice', 'read')")
    @GetMapping("/visibles")
    @Operation(
        summary = "Advices visibles ahora",
        description = "Filtra por la zona horaria indicada por cabeceras X-Timezone o X-Timezone-Offset"
    )
    public ResponseEntity<List<AdviceDTO>> getVisibleAdvicesNow(
            @RequestHeader(value = "X-Timezone", required = false)
            @Parameter(description = "Zona horaria IANA, p.ej. Europe/Madrid")
            String tz,
            @RequestHeader(value = "X-Timezone-Offset", required = false)
            @Parameter(description = "Minutos al ESTE de UTC, p.ej. 120")
            String offsetMinutesStr) {

        ZoneId zone = resolveZoneId(tz, offsetMinutesStr);
        logger.debug("Resolviendo visibles con zona: {}", zone);
        return ResponseEntity.ok(adviceService.getVisibleAdvicesNow(zone));
    }

    @PreAuthorize("@perm.can('advice', 'read')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener un advice por id")
    public ResponseEntity<AdviceDTO> getAdviceById(@PathVariable Long id) {
        Optional<AdviceDTO> advice = adviceService.getAdviceById(id);
        return advice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("@perm.can('advice', 'create')")
    @PostMapping
    @Operation(summary = "Crear un advice")
    public ResponseEntity<AdviceDTO> createAdvice(@RequestBody AdviceDTO adviceDTO) {
        return ResponseEntity.ok(adviceService.saveAdvice(adviceDTO));
    }

    @PreAuthorize("@perm.can('advice', 'update')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un advice")
    public ResponseEntity<AdviceDTO> updateAdvice(@PathVariable Long id, @RequestBody AdviceDTO adviceDTO) {
        logger.info("adviceDTO object: {}", adviceDTO);
        AdviceDTO updatedAdvice = adviceService.updateAdvice(id, adviceDTO);
        return ResponseEntity.ok(updatedAdvice);
    }

    @PreAuthorize("@perm.can('advice', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un advice")
    public ResponseEntity<Void> deleteAdvice(@PathVariable Long id) {
        adviceService.deleteAdvice(id);
        return ResponseEntity.noContent().build();
    }

    // ----------------- helpers -----------------
    private ZoneId resolveZoneId(String tz, String offsetMinutesStr) {
        if (tz != null && !tz.isBlank()) {
            try {
                return ZoneId.of(tz.trim());
            } catch (Exception e) {
                logger.warn("X-Timezone inválida '{}': {}", tz, e.getMessage());
            }
        }
        if (offsetMinutesStr != null && !offsetMinutesStr.isBlank()) {
            try {
                int minutes = Integer.parseInt(offsetMinutesStr.trim());
                return ZoneOffset.ofTotalSeconds(minutes * 60);
            } catch (Exception e) {
                logger.warn("X-Timezone-Offset inválido '{}': {}", offsetMinutesStr, e.getMessage());
            }
        }
        return ZoneId.systemDefault();
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/ApiKeyController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.ApiKeyService;
import com.screenleads.backend.app.domain.model.ApiKey;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiKey> createApiKey(@RequestParam Long clientDbId,
            @RequestParam String permissions,
            @RequestParam(defaultValue = "365") int daysValid) {
        ApiKey key = apiKeyService.createApiKeyByDbId(clientDbId, permissions, daysValid);
        return ResponseEntity.ok(key);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateApiKey(@PathVariable Long id) {
        apiKeyService.activateApiKey(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateApiKey(@PathVariable Long id) {
        apiKeyService.deactivateApiKey(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/client/{clientDbId}")
    public ResponseEntity<List<ApiKey>> getApiKeysByClient(@PathVariable Long clientDbId) {
        List<ApiKey> keys = apiKeyService.getApiKeysByClientDbId(clientDbId);
        return ResponseEntity.ok(keys);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/ApiKeyPermissionTestController.java
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
        
        if (auth.getPrincipal() instanceof ApiKeyPrincipal) {
            ApiKeyPrincipal principal = (ApiKeyPrincipal) auth.getPrincipal();
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

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/AppEntityController.java
package com.screenleads.backend.app.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.screenleads.backend.app.application.service.AppEntityService;
import com.screenleads.backend.app.web.dto.AppEntityDTO;
import com.screenleads.backend.app.web.dto.ReorderRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/entities")
@RequiredArgsConstructor
@Validated
@CrossOrigin // quítalo si no lo necesitas
public class AppEntityController {

    private final AppEntityService service;

    // ---- LISTAR ----
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appentity', 'read')")
    public ResponseEntity<List<AppEntityDTO>> list(
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        return ResponseEntity.ok(service.findAll(withCount));
    }

    // ---- OBTENER POR ID ----
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appentity', 'read')")
    public ResponseEntity<AppEntityDTO> getById(
            @PathVariable Long id,
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        return ResponseEntity.ok(service.findById(id, withCount));
    }

    // ---- OBTENER POR RESOURCE ----
    @GetMapping("/by-resource/{resource}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appentity', 'read')")
    public ResponseEntity<AppEntityDTO> getByResource(
            @PathVariable String resource,
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        return ResponseEntity.ok(service.findByResource(resource, withCount));
    }

    // ---- UPSERT (CREAR/ACTUALIZAR) ----
    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appentity', 'update')")
    public ResponseEntity<AppEntityDTO> upsert(@RequestBody AppEntityDTO dto) {
        AppEntityDTO saved = service.upsert(dto);
        return new ResponseEntity<>(saved, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appentity', 'update')")
    public ResponseEntity<AppEntityDTO> update(@PathVariable Long id, @RequestBody AppEntityDTO dto) {
        if (dto.id() != null && !dto.id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path id y body id no coinciden");
        }
        AppEntityDTO withId = dto.toBuilder().id(id).build();
        return ResponseEntity.ok(service.upsert(withId));
    }

    // ---- BORRAR ----
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appentity', 'delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteById(id);
    }

    // ---- REORDENAR ENTIDADES (drag & drop) ----
    @PutMapping("/reorder")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appentity', 'update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderEntities(@RequestBody ReorderRequest request) {
        service.reorderEntities(request.ids());
    }

    // ---- REORDENAR ATRIBUTOS DE UNA ENTIDAD (drag & drop) ----
    @PutMapping("/{id}/attributes/reorder")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appentity', 'update')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderAttributes(@PathVariable Long id, @RequestBody ReorderRequest request) {
        service.reorderAttributes(id, request.ids());
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/AppVersionController.java
// src/main/java/com/screenleads/backend/app/web/controller/AppVersionController.java
package com.screenleads.backend.app.web.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.AppVersionService;
import com.screenleads.backend.app.web.dto.AppVersionDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/app-versions")
@RequiredArgsConstructor
@Tag(name = "App Versions", description = "Gestión de versiones de la app por plataforma")
public class AppVersionController {

    private final AppVersionService service;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appversion', 'create')")
    @Operation(summary = "Crear versión")
    public AppVersionDTO save(@RequestBody AppVersionDTO dto) {
        return service.save(dto);
    }

    @GetMapping
    @PreAuthorize("@perm.can('appversion', 'read')")
    @Operation(summary = "Listar versiones")
    public List<AppVersionDTO> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.can('appversion', 'read')")
    @Operation(summary = "Obtener versión por id")
    public AppVersionDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appversion', 'delete')")
    @Operation(summary = "Eliminar versión por id")
    public void deleteById(@PathVariable Long id) {
        service.deleteById(id);
    }

    @GetMapping("/latest/{platform}")
    @PreAuthorize("@perm.can('appversion', 'read')")
    @Operation(summary = "Última versión por plataforma")
    public AppVersionDTO getLatestVersion(@PathVariable String platform) {
        return service.getLatestVersion(platform);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('appversion', 'update')")
    @Operation(summary = "Actualizar versión por id")
    public AppVersionDTO update(@PathVariable Long id, @RequestBody AppVersionDTO dto) {
        dto.setId(id);
        return service.save(dto);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/BillingController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.StripeBillingService;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {
    private final StripeBillingService billing;
    private final CompanyRepository companies;

    @PostMapping("/checkout-session/{companyId}")
    @PreAuthorize("hasRole('admin') or hasRole('company_admin')")
    public Map<String, String> createCheckout(@PathVariable Long companyId) throws Exception {
        var company = companies.findById(companyId).orElseThrow();
        String sessionId = billing.createCheckoutSession(company);
        return Map.of("id", sessionId);
    }

    @PostMapping("/portal-session/{companyId}")
    @PreAuthorize("hasRole('admin') or hasRole('company_admin')")
    public Map<String, String> portal(@PathVariable Long companyId) throws Exception {
        var company = companies.findById(companyId).orElseThrow();
        String url = billing.createBillingPortalSession(company);
        return Map.of("url", url);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/ClientController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.domain.model.Client;
import com.screenleads.backend.app.domain.repositories.ClientRepository;
import com.screenleads.backend.app.application.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/clients")
public class ClientController {
    private com.screenleads.backend.app.web.dto.ClientDTO toDTO(Client client) {
        com.screenleads.backend.app.web.dto.ClientDTO dto = new com.screenleads.backend.app.web.dto.ClientDTO();
        dto.setId(client.getId());
        dto.setClientId(client.getClientId());
        dto.setName(client.getName());
        dto.setActive(client.isActive());
        if (client.getApiKeys() != null) {
            java.util.List<com.screenleads.backend.app.web.dto.ApiKeyDTO> apiKeyDTOs = client.getApiKeys().stream()
                    .map(apiKey -> {
                        com.screenleads.backend.app.web.dto.ApiKeyDTO akDto = new com.screenleads.backend.app.web.dto.ApiKeyDTO();
                        akDto.setId(apiKey.getId());
                        akDto.setKey(apiKey.getKey());
                        akDto.setActive(apiKey.isActive());
                        akDto.setCreatedAt(apiKey.getCreatedAt());
                        akDto.setExpiresAt(apiKey.getExpiresAt());
                        akDto.setPermissions(apiKey.getPermissions());
                        return akDto;
                    }).toList();
            dto.setApiKeys(apiKeyDTOs);
        }
        return dto;
    }

    private ResponseEntity<Void> activate(Client client) {
        client.setActive(true);
        clientRepository.save(client);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Void> deactivate(Client client) {
        client.setActive(false);
        clientRepository.save(client);
        return ResponseEntity.ok().build();
    }

    private final ClientRepository clientRepository;

    private final ApiKeyService apiKeyService;

    public ClientController(ClientRepository clientRepository, ApiKeyService apiKeyService) {
        this.clientRepository = clientRepository;
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public ResponseEntity<java.util.List<com.screenleads.backend.app.web.dto.ClientDTO>> listClients() {
        java.util.List<Client> clients = clientRepository.findAll();
        java.util.List<com.screenleads.backend.app.web.dto.ClientDTO> dtos = clients.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        // Autogenerar clientId alfanumérico
        client.setClientId(java.util.UUID.randomUUID().toString());
        client.setActive(true);
        Client saved = clientRepository.save(client);
        // Crear la primera API Key con permisos básicos de lectura
        String defaultPermissions = "device:read,customer:read,promotion:read,advice:read,media:read";
        apiKeyService.createApiKeyByDbId(saved.getId(), defaultPermissions, 365);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @RequestBody Client client) {
        return clientRepository.findById(id)
                .map(existing -> {
                    existing.setName(client.getName());
                    existing.setClientId(client.getClientId());
                    existing.setActive(client.isActive());
                    return ResponseEntity.ok(clientRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(this::activate)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateClient(@PathVariable Long id) {
        return clientRepository.findById(id)
                .map(this::deactivate)
                .orElse(ResponseEntity.notFound().build());
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/CompanyController.java
// src/main/java/com/screenleads/backend/app/web/controller/CompanyController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.CompaniesService;
import com.screenleads.backend.app.web.dto.CompanyDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin
@RestController
@RequestMapping("/companies")
@Tag(name = "Companies", description = "CRUD de compañías")
public class CompanyController {

    private final CompaniesService companiesService;

    public CompanyController(CompaniesService companiesService) {
        this.companiesService = companiesService;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'read')")
    @GetMapping
    @Operation(summary = "Listar compañías")
    public ResponseEntity<List<CompanyDTO>> getAllCompanies() {
        return ResponseEntity.ok(companiesService.getAllCompanies());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'read')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener compañía por id")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long id) {
        Optional<CompanyDTO> company = companiesService.getCompanyById(id);
        return company.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'create')")
    @PostMapping
    @Operation(summary = "Crear compañía", description = "ROLE_ADMIN o permiso company:create")
    public ResponseEntity<CompanyDTO> createCompany(@RequestBody CompanyDTO companyDTO) {
        return ResponseEntity.ok(companiesService.saveCompany(companyDTO));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'update')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar compañía", description = "ROLE_ADMIN o permiso company:update")
    public ResponseEntity<CompanyDTO> updateCompany(@PathVariable Long id, @RequestBody CompanyDTO companyDTO) {
        CompanyDTO updatedCompany = companiesService.updateCompany(id, companyDTO);
        return ResponseEntity.ok(updatedCompany);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar compañía", description = "ROLE_ADMIN o permiso company:delete")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companiesService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/CompanyTokenController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.domain.model.CompanyToken;
import com.screenleads.backend.app.application.service.CompanyTokenService;
import com.screenleads.backend.app.web.dto.CompanyTokenDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/company-tokens")
public class CompanyTokenController {
    @PutMapping("/{id}")
    public ResponseEntity<CompanyTokenDTO> updateToken(@PathVariable Long id, @RequestBody CompanyTokenDTO dto) {
        Optional<CompanyToken> updated = companyTokenService.updateToken(id, dto);
        return updated.map(t -> ResponseEntity.ok(toDto(t))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyTokenDTO> getTokenById(@PathVariable Long id) {
        Optional<CompanyToken> token = companyTokenService.getTokenById(id);
        return token.map(t -> ResponseEntity.ok(toDto(t))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private final CompanyTokenService companyTokenService;

    public CompanyTokenController(CompanyTokenService companyTokenService) {
        this.companyTokenService = companyTokenService;
    }

    @PostMapping
    public ResponseEntity<CompanyTokenDTO> createToken(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) CompanyTokenDTO dto) {
        CompanyToken token = companyTokenService.createTokenForUser(userDetails.getUsername(),
                dto != null ? dto.getDescripcion() : null);
        return ResponseEntity.ok(toDto(token));
    }

    @GetMapping
    public ResponseEntity<List<CompanyTokenDTO>> getTokens(@AuthenticationPrincipal UserDetails userDetails) {
        List<CompanyToken> tokens = companyTokenService.getTokensForAuthenticatedUser(userDetails.getUsername());
        List<CompanyTokenDTO> dtos = tokens.stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToken(@PathVariable Long id) {
        companyTokenService.deleteToken(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{token}/renew")
    public ResponseEntity<CompanyTokenDTO> renewToken(@PathVariable String token) {
        Optional<CompanyToken> renewed = companyTokenService.renewToken(token);
        return renewed.map(t -> ResponseEntity.ok(toDto(t))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{token}/description")
    public ResponseEntity<CompanyTokenDTO> updateDescription(@PathVariable String token,
            @RequestBody CompanyTokenDTO dto) {
        Optional<CompanyToken> updated = companyTokenService.updateDescription(token, dto.getDescripcion());
        return updated.map(t -> ResponseEntity.ok(toDto(t))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private CompanyTokenDTO toDto(CompanyToken token) {
        return CompanyTokenDTO.builder()
                .id(token.getId())
                .companyId(token.getCompanyId())
                .token(token.getToken())
                .role(token.getRole())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .descripcion(token.getDescripcion())
                .build();
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/CouponController.java
package com.screenleads.backend.app.web.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.CouponService;
import com.screenleads.backend.app.domain.model.CouponStatus;
import com.screenleads.backend.app.domain.model.PromotionLead;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Validación y canje de cupones de promociones")
public class CouponController {

    private final CouponService couponService;

    // === GET /coupons/{code} -> validar ===
    @GetMapping("/{code}")
    @PreAuthorize("@perm.can('coupon', 'read')")
    @Operation(summary = "Validar cupón por código",
               description = "Devuelve el estado y si es válido en este momento")
    public ResponseEntity<CouponValidationResponse> validate(@PathVariable String code) {
        try {
            PromotionLead lead = couponService.validate(code);
            return ResponseEntity.ok(CouponValidationResponse.from(lead, true, null));
        } catch (Exception ex) {
            // No filtramos tipos para simplificar: el mensaje explica la causa
            return ResponseEntity.ok(new CouponValidationResponse(code, false, null, null, null, ex.getMessage()));
        }
    }

    // === POST /coupons/{code}/redeem -> canjear ===
    @PostMapping("/{code}/redeem")
    @PreAuthorize("@perm.can('coupon', 'update')")
    @Operation(summary = "Canjear cupón por código",
               description = "Marca el cupón como REDEEMED si es válido")
    public ResponseEntity<CouponValidationResponse> redeem(@PathVariable String code) {
        PromotionLead lead = couponService.redeem(code);
        return ResponseEntity.ok(CouponValidationResponse.from(lead, true, "REDEEMED"));
    }

    // === POST /coupons/{code}/expire -> caducar manualmente ===
    @PostMapping("/{code}/expire")
    @PreAuthorize("@perm.can('coupon', 'update')")
    @Operation(summary = "Caducar cupón por código",
               description = "Marca el cupón como EXPIRED si aún no se ha canjeado")
    public ResponseEntity<CouponValidationResponse> expire(@PathVariable String code) {
        PromotionLead lead = couponService.expire(code);
        return ResponseEntity.ok(CouponValidationResponse.from(lead, false, "EXPIRED"));
    }

    // === POST /coupons/issue?promotionId=&customerId= -> emitir ===
    @PostMapping("/issue")
    @PreAuthorize("@perm.can('coupon', 'create')")
    @Operation(summary = "Emitir cupón (crear lead histórico)",
               description = "Genera un nuevo cupón interno para un cliente y una promoción, respetando límites")
    public ResponseEntity<CouponValidationResponse> issue(
            @RequestParam Long promotionId,
            @RequestParam Long customerId) {
        PromotionLead lead = couponService.issueCoupon(promotionId, customerId);
        return ResponseEntity.ok(CouponValidationResponse.from(lead, true, "VALID"));
    }

    // ====== DTO de respuesta simple ======
    @Data
    @AllArgsConstructor
    static class CouponValidationResponse {
        private String couponCode;
        private boolean valid;
        private CouponStatus status;
        private Instant redeemedAt;
        private Instant expiresAt;
        private String message;

        static CouponValidationResponse from(PromotionLead lead, boolean valid, String msg) {
            return new CouponValidationResponse(
                lead.getCouponCode(),
                valid,
                lead.getCouponStatus(),
                lead.getRedeemedAt(),
                lead.getExpiresAt(),
                msg
            );
        }
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/CustomerController.java
package com.screenleads.backend.app.web.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.CustomerService;
import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@RestController
@RequestMapping("/customers")
@Tag(name = "Customers", description = "CRUD de clientes que participan en promociones")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ===== Listar por company (opcional) + búsqueda parcial en identifier =====
    @PreAuthorize("@perm.can('customer', 'read')")
    @GetMapping
    @Operation(summary = "Listar clientes", description = "Filtra por companyId y búsqueda parcial en identifier")
    public ResponseEntity<List<CustomerResponse>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search) {

        List<Customer> result = customerService.list(companyId, search);
        return ResponseEntity.ok(result.stream().map(CustomerResponse::from).toList());
    }

    // ===== Obtener por id =====
    @PreAuthorize("@perm.can('customer', 'read')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por id")
    public ResponseEntity<CustomerResponse> get(@PathVariable Long id) {
        Customer c = customerService.get(id);
        return ResponseEntity.ok(CustomerResponse.from(c));
    }

    // ===== Crear =====
    @PreAuthorize("@perm.can('customer', 'create')")
    @PostMapping
    @Operation(summary = "Crear cliente", description = "Crea un cliente normalizando el identificador y aplicando unicidad por empresa")
    public ResponseEntity<CustomerResponse> create(@RequestBody CreateRequest req) {
        Customer c = customerService.create(
                req.getCompanyId(),
                req.getIdentifierType(),
                req.getIdentifier(),
                req.getFirstName(),
                req.getLastName());
        return ResponseEntity
                .created(URI.create("/customers/" + c.getId()))
                .body(CustomerResponse.from(c));
    }

    // ===== Actualizar =====
    @PreAuthorize("@perm.can('customer', 'update')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente")
    public ResponseEntity<CustomerResponse> update(@PathVariable Long id, @RequestBody UpdateRequest req) {
        Customer c = customerService.update(
                id,
                req.getIdentifierType(),
                req.getIdentifier(),
                req.getFirstName(),
                req.getLastName());
        return ResponseEntity.ok(CustomerResponse.from(c));
    }

    // ===== Borrar =====
    @PreAuthorize("@perm.can('customer', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Borrar cliente")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== DTOs ====================

    @Data
    public static class CreateRequest {
        @NotNull
        private Long companyId;

        @NotNull
        private LeadIdentifierType identifierType;

        @NotBlank
        private String identifier;

        private String firstName;
        private String lastName;
    }

    @Data
    public static class UpdateRequest {
        @NotNull
        private LeadIdentifierType identifierType;

        @NotBlank
        private String identifier;

        private String firstName;
        private String lastName;
    }

    @Value
    @Builder
    public static class CustomerResponse {
        Long id;
        Long companyId;
        LeadIdentifierType identifierType;
        String identifier;
        String firstName;
        String lastName;

        public static CustomerResponse from(Customer c) {
            return CustomerResponse.builder()
                    .id(c.getId())
                    .companyId(c.getCompany().getId())
                    .identifierType(c.getIdentifierType())
                    .identifier(c.getIdentifier())
                    .firstName(c.getFirstName())
                    .lastName(c.getLastName())
                    .build();
        }
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/DeviceTypesController.java
// src/main/java/com/screenleads/backend/app/web/controller/DeviceTypesController.java
package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.DeviceTypeService;
import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController // <-- era @Controller; @RestController no cambia las rutas existentes
@Tag(name = "Device Types", description = "CRUD de tipos de dispositivo")
public class DeviceTypesController {

    @Autowired
    private DeviceTypeService deviceTypeService;

    public DeviceTypesController(DeviceTypeService deviceTypeService) {
        this.deviceTypeService = deviceTypeService;
    }

    @CrossOrigin
    @GetMapping("/devices/types")
    @PreAuthorize("@perm.can('devicetype', 'read')")
    @Operation(summary = "Listar tipos de dispositivo")
    public ResponseEntity<List<DeviceTypeDTO>> getAllDeviceTypes() {
        return ResponseEntity.ok(deviceTypeService.getAllDeviceTypes());
    }

    @CrossOrigin
    @GetMapping("/devices/types/{id}")
    @PreAuthorize("@perm.can('devicetype', 'read')")
    @Operation(summary = "Obtener tipo de dispositivo por id")
    public ResponseEntity<DeviceTypeDTO> getDeviceTypeById(@PathVariable Long id) {
        Optional<DeviceTypeDTO> device = deviceTypeService.getDeviceTypeById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/devices/types")
    @PreAuthorize("@perm.can('devicetype', 'create')")
    @Operation(summary = "Crear tipo de dispositivo")
    public ResponseEntity<DeviceTypeDTO> createDeviceType(@RequestBody DeviceTypeDTO deviceDTO) {
        return ResponseEntity.ok(deviceTypeService.saveDeviceType(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/devices/types/{id}")
    @PreAuthorize("@perm.can('devicetype', 'update')")
    @Operation(summary = "Actualizar tipo de dispositivo")
    public ResponseEntity<DeviceTypeDTO> updateDeviceType(@PathVariable Long id, @RequestBody DeviceTypeDTO deviceDTO) {
        DeviceTypeDTO updatedDevice = deviceTypeService.updateDeviceType(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);
    }

    @CrossOrigin
    @DeleteMapping("/devices/types/{id}")
    @PreAuthorize("@perm.can('devicetype', 'delete')")
    @Operation(summary = "Eliminar tipo de dispositivo")
    public ResponseEntity<String> deleteDeviceType(@PathVariable Long id) {
        deviceTypeService.deleteDeviceType(id);
        return ResponseEntity.ok("Media Type (" + id + ") deleted succesfully");
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/DevicesController.java
// src/main/java/com/screenleads/backend/app/web/controller/DevicesController.java
package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.DeviceService;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/devices")
@CrossOrigin
@Tag(name = "Devices", description = "CRUD de dispositivos y gestión de advices por dispositivo")
public class DevicesController {

    private final DeviceService deviceService;

    public DevicesController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    // -------------------------------------------------------------------------
    // CRUD básico
    // -------------------------------------------------------------------------

    @PreAuthorize("@perm.can('device', 'read')")
    @GetMapping
    @Operation(summary = "Listar dispositivos")
    public ResponseEntity<List<DeviceDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @PreAuthorize("@perm.can('device', 'read')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener dispositivo por id")
    public ResponseEntity<DeviceDTO> getDeviceById(@PathVariable Long id) {
        Optional<DeviceDTO> device = deviceService.getDeviceById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("@perm.can('device', 'create')")
    @PostMapping
    @Operation(summary = "Crear dispositivo")
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody DeviceDTO deviceDTO) {
        DeviceDTO saved = deviceService.saveDevice(deviceDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("@perm.can('device', 'update')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar dispositivo")
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable Long id, @RequestBody DeviceDTO deviceDTO) {
        DeviceDTO updatedDevice = deviceService.updateDevice(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);
    }

    @PreAuthorize("@perm.can('device', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar dispositivo")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Búsqueda / existencia por UUID (para que el frontend se "autocure")
    // -------------------------------------------------------------------------

    @PreAuthorize("@perm.can('device', 'read')")
    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "Obtener dispositivo por UUID")
    public ResponseEntity<DeviceDTO> getDeviceByUuid(@PathVariable String uuid) {
        return deviceService.getDeviceByUuid(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @RequestMapping(value = "/uuid/{uuid}", method = RequestMethod.HEAD)
    @Operation(summary = "Comprobar existencia de dispositivo por UUID", description = "Devuelve 200 si existe, 404 si no")
    public ResponseEntity<Void> headDeviceByUuid(@PathVariable String uuid) {
        return deviceService.getDeviceByUuid(uuid).isPresent()
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // -------------------------------------------------------------------------
    // Gestión de advices por dispositivo
    // -------------------------------------------------------------------------

    @GetMapping("/{deviceId}/advices")
    @Operation(summary = "Listar advices asignados a un dispositivo")
    public ResponseEntity<List<AdviceDTO>> getAdvicesForDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(deviceService.getAdvicesForDevice(deviceId));
    }

    @PostMapping("/{deviceId}/advices/{adviceId}")
    @Operation(summary = "Asignar un advice a un dispositivo")
    public ResponseEntity<Void> assignAdviceToDevice(@PathVariable Long deviceId, @PathVariable Long adviceId) {
        deviceService.assignAdviceToDevice(deviceId, adviceId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{deviceId}/advices/{adviceId}")
    @Operation(summary = "Quitar un advice de un dispositivo")
    public ResponseEntity<Void> removeAdviceFromDevice(@PathVariable Long deviceId, @PathVariable Long adviceId) {
        deviceService.removeAdviceFromDevice(deviceId, adviceId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // (Opcional) Endpoint placeholder para /code/{uuid} si realmente lo necesitas
    // -------------------------------------------------------------------------
    /**
     * TODO: Implementar generación/lectura de código de conexión para el dispositivo.
     */
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/MediaController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.FirebaseStorageService;
import com.screenleads.backend.app.application.service.MediaService;
import com.screenleads.backend.app.web.dto.MediaDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;

@RestController // <-- antes era @Controller
@Slf4j
public class MediaController {

    @Autowired private FirebaseStorageService firebaseService;
    @Autowired private MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    // ---------------- LIST/CRUD ----------------

    @PreAuthorize("@perm.can('media', 'read')")
    @CrossOrigin
    @GetMapping("/medias")
    public ResponseEntity<List<MediaDTO>> getAllMedias() {
        return ResponseEntity.ok(mediaService.getAllMedias());
    }

    // ---------------- UPLOAD ----------------

    @PreAuthorize("@perm.can('media', 'create')")
    @CrossOrigin
    @PostMapping(
        value = "/medias/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> upload(@RequestPart("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Archivo vacío"));
            }

            final String original = Optional.ofNullable(file.getOriginalFilename()).orElse("upload.bin");
            final String safeName = original.replaceAll("[^A-Za-z0-9._-]", "_");
            final String fileName  = UUID.randomUUID() + "-" + safeName;
            final String rawPath   = "raw/" + fileName;

            // /tmp es el disco efímero de Heroku
            Path tmpDir = Paths.get(Optional.ofNullable(System.getProperty("java.io.tmpdir")).orElse("/tmp"));
            Files.createDirectories(tmpDir);
            Path tmp = Files.createTempFile(tmpDir, "upload_", "_" + safeName);

            log.info("📥 Recibido multipart: name={}, size={} bytes, contentType={}",
                    safeName, file.getSize(), file.getContentType());

            // copiar a tmp
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }

            // subir a Firebase (raw/)
            firebaseService.upload(tmp.toFile(), rawPath);
            log.info("📤 Subido a Firebase RAW: {}", rawPath);

            // opcional: borrar temporal
            try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}

            // 200 OK con filename (el front hace polling a /medias/status/{filename})
            return ResponseEntity.ok(Map.of("filename", fileName));

        } catch (MaxUploadSizeExceededException tooBig) {
            return ResponseEntity.status(413).body(Map.of("error", "Archivo demasiado grande"));
        } catch (Exception ex) {
            log.error("❌ Error subiendo archivo", ex);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Fallo subiendo archivo",
                "detail", ex.getMessage()
            ));
        }
    }

    @CrossOrigin
    @GetMapping(value = "/medias/status/{filename}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> checkCompressionStatus(@PathVariable String filename) {
        log.info("📡 Comprobando estado de compresión: {}", filename);

        final String IMG_DEST = "media/images";
        final String VID_DEST = "media/videos";
        final int[] IMAGE_THUMBS = {320, 640};
        final int[] VIDEO_THUMBS = {320, 640};

        String base = stripExtension(filename);
        MediaKind kind = detectKind(filename);

        // 1) compatibilidad legacy
        String legacyPath = "media/compressed-" + filename;
        if (firebaseService.exists(legacyPath)) {
            return ResponseEntity.ok(Map.of(
                "status", "ready",
                "type", "legacy",
                "url", firebaseService.getPublicUrl(legacyPath),
                "thumbnails", List.of()
            ));
        }

        // 2) rutas nuevas
        List<String> mainCandidates = new ArrayList<>();
        List<String> thumbCandidates = new ArrayList<>();

        if (kind == MediaKind.VIDEO) {
            mainCandidates.add(VID_DEST + "/compressed-" + base + ".mp4");
            for (int s : VIDEO_THUMBS) {
                thumbCandidates.add(VID_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".jpg");
            }
        } else if (kind == MediaKind.IMAGE) {
            mainCandidates.add(IMG_DEST + "/compressed-" + base + ".jpg");
            mainCandidates.add(IMG_DEST + "/compressed-" + base + ".png");
            for (int s : IMAGE_THUMBS) {
                thumbCandidates.add(IMG_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".jpg");
                thumbCandidates.add(IMG_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".png");
            }
        } else {
            mainCandidates.add(VID_DEST + "/compressed-" + base + ".mp4");
            mainCandidates.add(IMG_DEST + "/compressed-" + base + ".jpg");
            mainCandidates.add(IMG_DEST + "/compressed-" + base + ".png");
            for (int s : new int[]{320, 640}) {
                thumbCandidates.add(VID_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".jpg");
                thumbCandidates.add(IMG_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".jpg");
                thumbCandidates.add(IMG_DEST + "/thumbnails/" + s + "/thumb-" + s + "-" + base + ".png");
            }
        }

        String foundMain = null;
        for (String cand : mainCandidates) {
            if (firebaseService.exists(cand)) { foundMain = cand; break; }
        }

        if (foundMain != null) {
            List<String> thumbs = new ArrayList<>();
            for (String t : thumbCandidates) {
                if (firebaseService.exists(t)) thumbs.add(firebaseService.getPublicUrl(t));
            }
            String type = foundMain.startsWith(VID_DEST) ? "video"
                        : foundMain.startsWith(IMG_DEST) ? "image" : "unknown";

            return ResponseEntity.ok(Map.of(
                "status", "ready",
                "type", type,
                "url", firebaseService.getPublicUrl(foundMain),
                "thumbnails", thumbs
            ));
        }
        return ResponseEntity.status(202).body(Map.of("status", "processing"));
    }

    private enum MediaKind { VIDEO, IMAGE, UNKNOWN }

    private MediaKind detectKind(String filename) {
        String f = filename.toLowerCase();
        if (f.endsWith(".mp4") || f.endsWith(".mov") || f.endsWith(".webm")) return MediaKind.VIDEO;
        if (f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png")
         || f.endsWith(".webp") || f.endsWith(".avif") || f.endsWith(".heic")
         || f.endsWith(".heif") || f.endsWith(".gif")) return MediaKind.IMAGE;
        return MediaKind.UNKNOWN;
    }
    private String stripExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(0, i) : filename;
    }

    // ---------------- CRUD restantes (sin tocar rutas) ----------------

    @CrossOrigin
    @GetMapping("/medias/{id}")
    public ResponseEntity<MediaDTO> getMediaById(@PathVariable Long id) {
        return mediaService.getMediaById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/medias")
    public ResponseEntity<MediaDTO> createMedia(@RequestBody MediaDTO deviceDTO) {
        return ResponseEntity.ok(mediaService.saveMedia(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/medias/{id}")
    public ResponseEntity<MediaDTO> updateMedia(@PathVariable Long id, @RequestBody MediaDTO deviceDTO) {
        return ResponseEntity.ok(mediaService.updateMedia(id, deviceDTO));
    }

    @CrossOrigin
    @DeleteMapping("/medias/{id}")
    public ResponseEntity<String> deleteMedia(@PathVariable Long id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }

    @CrossOrigin
    @GetMapping("/medias/render/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) throws Exception {
        Optional<MediaDTO> mediaaux = mediaService.getMediaById(id);
        Path filePath = Paths.get("src/main/resources/static/medias/").resolve(mediaaux.get().src()).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/MediaTypesController.java
package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.screenleads.backend.app.application.service.MediaTypeService;
import com.screenleads.backend.app.web.dto.MediaTypeDTO;

@Controller
public class MediaTypesController {
    @Autowired
    private MediaTypeService deviceTypeService;

    public MediaTypesController(MediaTypeService deviceTypeService) {
        this.deviceTypeService = deviceTypeService;
    }

    @CrossOrigin
    @GetMapping("/medias/types")
    @PreAuthorize("@perm.can('mediatype', 'read')")
    public ResponseEntity<List<MediaTypeDTO>> getAllMediaTypes() {
        return ResponseEntity.ok(deviceTypeService.getAllMediaTypes());
    }

    @CrossOrigin
    @GetMapping("/medias/types/{id}")
    @PreAuthorize("@perm.can('mediatype', 'read')")
    public ResponseEntity<MediaTypeDTO> getMediaTypeById(@PathVariable Long id) {
        Optional<MediaTypeDTO> device = deviceTypeService.getMediaTypeById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/medias/types")
    @PreAuthorize("@perm.can('mediatype', 'create')")
    public ResponseEntity<MediaTypeDTO> createMediaType(@RequestBody MediaTypeDTO deviceDTO) {
        return ResponseEntity.ok(deviceTypeService.saveMediaType(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/medias/types/{id}")
    @PreAuthorize("@perm.can('mediatype', 'update')")
    public ResponseEntity<MediaTypeDTO> updateMediaType(@PathVariable Long id, @RequestBody MediaTypeDTO deviceDTO) {

        MediaTypeDTO updatedDevice = deviceTypeService.updateMediaType(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);

    }

    @CrossOrigin
    @DeleteMapping("/medias/types/{id}")
    @PreAuthorize("@perm.can('mediatype', 'delete')")
    public ResponseEntity<String> deleteMediaType(@PathVariable Long id) {
        deviceTypeService.deleteMediaType(id);
        return ResponseEntity.ok("Media Type (" + id + ") deleted succesfully");
    }
}
```

```java
// src/main/java/com/screenleads/backend/app/web/controller/PromotionsController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.PromotionService;
import com.screenleads.backend.app.web.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/promotions")
public class PromotionsController {

    @Autowired
    private PromotionService promotionService;

    // ===== CRUD =====
    @PreAuthorize("@perm.can('promotion', 'read')")
    @GetMapping
    public List<PromotionDTO> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @PreAuthorize("@perm.can('promotion', 'read')")
    @GetMapping("/{id}")
    public PromotionDTO getPromotionById(@PathVariable Long id) {
        return promotionService.getPromotionById(id);
    }

    @PreAuthorize("@perm.can('promotion', 'create')")
    @PostMapping
    public PromotionDTO createPromotion(@RequestBody PromotionDTO promotionDTO) {
        return promotionService.savePromotion(promotionDTO);
    }

    @PreAuthorize("@perm.can('promotion', 'update')")
    @PutMapping("/{id}")
    public PromotionDTO updatePromotion(@PathVariable Long id, @RequestBody PromotionDTO promotionDTO) {
        return promotionService.updatePromotion(id, promotionDTO);
    }

    @PreAuthorize("@perm.can('promotion', 'delete')")
    @DeleteMapping("/{id}")
    public void deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
    }

    // ===== Leads =====
    @PreAuthorize("@perm.can('lead', 'create')")
    @PostMapping("/{id}/leads")
    public PromotionLeadDTO registerLead(@PathVariable Long id, @RequestBody PromotionLeadDTO leadDTO) {
        return promotionService.registerLead(id, leadDTO);
    }

    @PreAuthorize("@perm.can('lead', 'read')")
    @GetMapping("/{id}/leads")
    public List<PromotionLeadDTO> listLeads(@PathVariable Long id) {
        return promotionService.listLeads(id);
    }

    // ===== Lead de prueba =====
    @PreAuthorize("@perm.can('lead', 'create')")
    @PostMapping("/{id}/leads/test")
    public PromotionLeadDTO createTestLead(
            @PathVariable Long id,
            @RequestBody(required = false) PromotionLeadDTO overrides) {
        return promotionService.createTestLead(id, overrides);
    }

    // ===== Export CSV (Streaming) =====
    @PreAuthorize("@perm.can('lead', 'read')")
    @GetMapping(value = "/{id}/leads/export.csv", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportLeadsCsv(
            @PathVariable Long id,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        ZonedDateTime toZdt = parseZdtOrDefault(to, ZonedDateTime.now(ZoneId.of("Europe/Madrid")));
        ZonedDateTime fromZdt = parseZdtOrDefault(from, toZdt.minusDays(30));

        StreamingResponseBody body = outputStream -> {
            String csv = promotionService.exportLeadsCsv(id, fromZdt, toZdt);
            outputStream.write(csv.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        };

        String filename = "promotion-" + id + "-leads.csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    // ===== Resumen JSON =====
    @GetMapping("/{id}/leads/summary")
    public LeadSummaryDTO getLeadSummary(
            @PathVariable Long id,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        ZonedDateTime toZdt = parseZdtOrDefault(to, ZonedDateTime.now(ZoneId.of("Europe/Madrid")));
        ZonedDateTime fromZdt = parseZdtOrDefault(from, toZdt.minusDays(30));
        return promotionService.getLeadSummary(id, fromZdt, toZdt);
    }

    private ZonedDateTime parseZdtOrDefault(String s, ZonedDateTime defaultValue) {
        if (s == null || s.isBlank())
            return defaultValue;
        try {
            if (s.length() == 10) {
                LocalDate d = LocalDate.parse(s);
                return d.atStartOfDay(ZoneId.of("Europe/Madrid"));
            }
            return ZonedDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return defaultValue;
        }
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/RoleController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.RoleService;
import com.screenleads.backend.app.application.service.PermissionService; // <-- IMPORT CORRECTO
import com.screenleads.backend.app.web.dto.RoleDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService service;
    private final PermissionService perm; // inyectamos el bean real

    public RoleController(RoleService service, PermissionService perm) {
        this.service = service;
        this.perm = perm;
    }

    @GetMapping
    @PreAuthorize("@perm.can('user','read')")
    public ResponseEntity<List<RoleDTO>> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.can('user','read')")
    public ResponseEntity<RoleDTO> get(@PathVariable Long id) {
        RoleDTO dto = service.getById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @PreAuthorize("@perm.can('user','update')")
    public ResponseEntity<RoleDTO> create(@RequestBody RoleDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('user','update')")
    public ResponseEntity<RoleDTO> update(@PathVariable Long id, @RequestBody RoleDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('user','delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Roles asignables = roles con level >= nivel efectivo del solicitante.
     * Requiere permiso de crear o actualizar usuarios.
     */
    @GetMapping("/assignable")
    @PreAuthorize("@perm.can('user','create') or @perm.can('user','update')")
    public ResponseEntity<List<RoleDTO>> assignable() {
        int myLevel = perm.effectiveLevel();
        List<RoleDTO> all = service.getAll();
        List<RoleDTO> allowed = all.stream()
                .filter(r -> r.level() != null && r.level() >= myLevel)
                .toList();
        return ResponseEntity.ok(allowed);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/UserController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.UserService;
import com.screenleads.backend.app.web.dto.UserDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
// @PreAuthorize("hasAnyRole('admin','company_admin')")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('user','read')")
    public ResponseEntity<List<UserDto>> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('user','read')")
    public ResponseEntity<UserDto> get(@PathVariable Long id) {
        UserDto dto = service.getById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('user','create')")
    public ResponseEntity<?> create(@RequestBody UserDto dto) {
        try {
            com.screenleads.backend.app.web.dto.UserCreationResponse created = service.create(dto);
            return ResponseEntity.status(HttpStatus.OK).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Violación de integridad (¿username/email único?)"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo crear el usuario"));
        }
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('user','update')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UserDto dto) {
        try {
            UserDto updated = service.update(id, dto);
            return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Violación de integridad (¿username/email único?)"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo actualizar el usuario"));
        }
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('user','delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/WebSocketStatusController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.infraestructure.websocket.PresenceChannelInterceptor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/ws")
@CrossOrigin(origins = "*")
public class WebSocketStatusController {

    @GetMapping("/status")
    public Map<String, Set<String>> getActiveRooms() {
        return PresenceChannelInterceptor.getActiveRooms();
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/WebsocketController.java
package com.screenleads.backend.app.web.controller;

import java.time.Instant;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.screenleads.backend.app.domain.model.ChatMessage;

@Controller
public class WebsocketController {
    @CrossOrigin
    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/{roomId}")
    public ChatMessage chat(@DestinationVariable String roomId, ChatMessage message) {
        message.setRoomId(roomId);
        message.setTimestamp(Instant.now());
        return message;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/WsCommandController.java
package com.screenleads.backend.app.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.WebSocketService;
import com.screenleads.backend.app.domain.model.ChatMessage;

@RestController
@RequestMapping("/ws") // 👈 Alineado con /ws/status
@CrossOrigin(origins = "*") // ajusta orígenes si quieres
public class WsCommandController {

    private final WebSocketService service;

    public WsCommandController(WebSocketService service) {
        this.service = service;
    }

    // ---- Endpoint que espera tu frontend ----
    @PostMapping("/command/{roomId}")
    public ResponseEntity<String> sendCommand(
            @PathVariable String roomId,
            @RequestBody ChatMessage message) {

        if (message.getId() == null || message.getId().isBlank()) {
            message.setId(java.util.UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(java.time.Instant.now());
        }

        System.out.printf("[WsCommandController] POST command room=%s id=%s type=%s msg=%s%n",
                roomId, message.getId(), message.getType(), message.getMessage());

        service.notifyFrontend(message, roomId);
        return ResponseEntity.accepted().body("202");
    }

    // ---- Endpoints de test que ya tenías (opcional mantenerlos) ----

    @PostMapping("/test/{roomId}")
    public ResponseEntity<String> test(@PathVariable String roomId, @RequestBody ChatMessage message) {
        System.out.println("[WsCommandController] test hit room=" + roomId);
        return ResponseEntity.ok("200");
    }

    @PostMapping("/test-message/{roomId}")
    public ResponseEntity<String> sendMessageTest(
            @PathVariable String roomId,
            @RequestBody ChatMessage message) {
        System.out.println("[WsCommandController] test-message room=" + roomId);
        service.notifyFrontend(message, roomId);
        return ResponseEntity.ok("200");
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/auth/AuthController.java
// src/main/java/com/screenleads/backend/app/web/controller/auth/AuthController.java
package com.screenleads.backend.app.web.controller.auth;

import com.screenleads.backend.app.application.security.AuthenticationService;
import com.screenleads.backend.app.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticación y cuentas de usuario")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Devuelve access/refresh token y datos básicos del usuario", security = {})
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PreAuthorize("@authSecurityChecker.allowRegister()")
    @PostMapping("/register")
    @Operation(summary = "Registro de usuario", security = {})
    public ResponseEntity<JwtResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PreAuthorize("@authSecurityChecker.isAuthenticated()")
    @PostMapping("/change-password")
    @Operation(summary = "Cambiar contraseña")
    public ResponseEntity<Void> changePassword(@RequestBody PasswordChangeRequest request) {
        authenticationService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @PreAuthorize("@authSecurityChecker.isAuthenticated()")
    @Operation(summary = "Usuario actual (requiere token)")
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok(authenticationService.getCurrentUser());
    }

    // ====== VARIANTE A: refresh PÚBLICO (coincide con tu SecurityConfig permitAll)
    // ======
    // Quita @PreAuthorize y marca security = {} para que en Swagger no pida token.
    // **Usa esta variante si /auth/refresh no requiere estar autenticado**
    /*
     * @PostMapping("/refresh")
     * 
     * @Operation(summary = "Refresh token", description =
     * "Devuelve un nuevo access token", security = {})
     * public ResponseEntity<JwtResponse> refreshToken() {
     * return ResponseEntity.ok(authenticationService.refreshToken());
     * }
     */

    // ====== VARIANTE B: refresh PROTEGIDO (si decides exigir autenticación) ======
    // **Usa esta variante si /auth/refresh debe requerir token**
    @PreAuthorize("@authSecurityChecker.isAuthenticated()")
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token (requiere token)")
    public ResponseEntity<JwtResponse> refreshTokenProtected() {
        return ResponseEntity.ok(authenticationService.refreshToken());
    }
}

```

