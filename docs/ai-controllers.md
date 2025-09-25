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

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un advice por id")
    public ResponseEntity<AdviceDTO> getAdviceById(@PathVariable Long id) {
        Optional<AdviceDTO> advice = adviceService.getAdviceById(id);
        return advice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear un advice")
    public ResponseEntity<AdviceDTO> createAdvice(@RequestBody AdviceDTO adviceDTO) {
        return ResponseEntity.ok(adviceService.saveAdvice(adviceDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un advice")
    public ResponseEntity<AdviceDTO> updateAdvice(@PathVariable Long id, @RequestBody AdviceDTO adviceDTO) {
        logger.info("adviceDTO object: {}", adviceDTO);
        AdviceDTO updatedAdvice = adviceService.updateAdvice(id, adviceDTO);
        return ResponseEntity.ok(updatedAdvice);
    }

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
// src/main/java/com/screenleads/backend/app/web/controller/AppEntityController.java
package com.screenleads.backend.app.web.controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.AppEntityService;
import com.screenleads.backend.app.web.dto.AppEntityDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/entities")
@RequiredArgsConstructor
@Validated
public class AppEntityController {

    private final AppEntityService service;

    // ---- LISTAR ----
    @GetMapping
    public ResponseEntity<List<AppEntityDTO>> list(
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        return ResponseEntity.ok(service.findAll(withCount));
    }

    // ---- OBTENER POR ID ----
    @GetMapping("/{id}")
    public ResponseEntity<AppEntityDTO> getById(
            @PathVariable Long id,
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        return ResponseEntity.ok(service.findById(id, withCount));
    }

    // ---- OBTENER POR RESOURCE ----
    @GetMapping("/by-resource/{resource}")
    public ResponseEntity<AppEntityDTO> getByResource(
            @PathVariable String resource,
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        return ResponseEntity.ok(service.findByResource(resource, withCount));
    }

    // ---- UPSERT (CREAR/ACTUALIZAR) ----
    @PutMapping
    public ResponseEntity<AppEntityDTO> upsert(@RequestBody AppEntityDTO dto) {
        AppEntityDTO saved = service.upsert(dto);
        return new ResponseEntity<>(saved, HttpStatus.OK);
    }

    // ---- BORRAR ----
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteById(id);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/AppVersionController.java
// src/main/java/com/screenleads/backend/app/web/controller/AppVersionController.java
package com.screenleads.backend.app.web.controller;

import java.util.List;

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
    @Operation(summary = "Crear versión")
    public AppVersionDTO save(@RequestBody AppVersionDTO dto) {
        return service.save(dto);
    }

    @GetMapping
    @Operation(summary = "Listar versiones")
    public List<AppVersionDTO> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener versión por id")
    public AppVersionDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar versión por id")
    public void deleteById(@PathVariable Long id) {
        service.deleteById(id);
    }

    @GetMapping("/latest/{platform}")
    @Operation(summary = "Última versión por plataforma")
    public AppVersionDTO getLatestVersion(@PathVariable String platform) {
        return service.getLatestVersion(platform);
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

    @GetMapping
    @Operation(summary = "Listar compañías")
    public ResponseEntity<List<CompanyDTO>> getAllCompanies() {
        return ResponseEntity.ok(companiesService.getAllCompanies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener compañía por id")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long id) {
        Optional<CompanyDTO> company = companiesService.getCompanyById(id);
        return company.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear compañía", description = "Solo ROLE_ADMIN")
    public ResponseEntity<CompanyDTO> createCompany(@RequestBody CompanyDTO companyDTO) {
        return ResponseEntity.ok(companiesService.saveCompany(companyDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar compañía", description = "Solo ROLE_ADMIN")
    public ResponseEntity<CompanyDTO> updateCompany(@PathVariable Long id, @RequestBody CompanyDTO companyDTO) {
        CompanyDTO updatedCompany = companiesService.updateCompany(id, companyDTO);
        return ResponseEntity.ok(updatedCompany);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Eliminar compañía", description = "Solo ROLE_ADMIN")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companiesService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/controller/CouponController.java
package com.screenleads.backend.app.web.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Canjear cupón por código",
               description = "Marca el cupón como REDEEMED si es válido")
    public ResponseEntity<CouponValidationResponse> redeem(@PathVariable String code) {
        PromotionLead lead = couponService.redeem(code);
        return ResponseEntity.ok(CouponValidationResponse.from(lead, true, "REDEEMED"));
    }

    // === POST /coupons/{code}/expire -> caducar manualmente ===
    @PostMapping("/{code}/expire")
    @Operation(summary = "Caducar cupón por código",
               description = "Marca el cupón como EXPIRED si aún no se ha canjeado")
    public ResponseEntity<CouponValidationResponse> expire(@PathVariable String code) {
        PromotionLead lead = couponService.expire(code);
        return ResponseEntity.ok(CouponValidationResponse.from(lead, false, "EXPIRED"));
    }

    // === POST /coupons/issue?promotionId=&customerId= -> emitir ===
    @PostMapping("/issue")
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
    @GetMapping
    @Operation(summary = "Listar clientes", description = "Filtra por companyId y búsqueda parcial en identifier")
    public ResponseEntity<List<CustomerResponse>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search) {

        List<Customer> result = customerService.list(companyId, search);
        return ResponseEntity.ok(result.stream().map(CustomerResponse::from).toList());
    }

    // ===== Obtener por id =====
    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por id")
    public ResponseEntity<CustomerResponse> get(@PathVariable Long id) {
        Customer c = customerService.get(id);
        return ResponseEntity.ok(CustomerResponse.from(c));
    }

    // ===== Crear =====
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
    @Operation(summary = "Listar tipos de dispositivo")
    public ResponseEntity<List<DeviceTypeDTO>> getAllDeviceTypes() {
        return ResponseEntity.ok(deviceTypeService.getAllDeviceTypes());
    }

    @CrossOrigin
    @GetMapping("/devices/types/{id}")
    @Operation(summary = "Obtener tipo de dispositivo por id")
    public ResponseEntity<DeviceTypeDTO> getDeviceTypeById(@PathVariable Long id) {
        Optional<DeviceTypeDTO> device = deviceTypeService.getDeviceTypeById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/devices/types")
    @Operation(summary = "Crear tipo de dispositivo")
    public ResponseEntity<DeviceTypeDTO> createDeviceType(@RequestBody DeviceTypeDTO deviceDTO) {
        return ResponseEntity.ok(deviceTypeService.saveDeviceType(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/devices/types/{id}")
    @Operation(summary = "Actualizar tipo de dispositivo")
    public ResponseEntity<DeviceTypeDTO> updateDeviceType(@PathVariable Long id, @RequestBody DeviceTypeDTO deviceDTO) {
        DeviceTypeDTO updatedDevice = deviceTypeService.updateDeviceType(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);
    }

    @CrossOrigin
    @DeleteMapping("/devices/types/{id}")
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

    @GetMapping
    @Operation(summary = "Listar dispositivos")
    public ResponseEntity<List<DeviceDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener dispositivo por id")
    public ResponseEntity<DeviceDTO> getDeviceById(@PathVariable Long id) {
        Optional<DeviceDTO> device = deviceService.getDeviceById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear dispositivo")
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody DeviceDTO deviceDTO) {
        DeviceDTO saved = deviceService.saveDevice(deviceDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar dispositivo")
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable Long id, @RequestBody DeviceDTO deviceDTO) {
        DeviceDTO updatedDevice = deviceService.updateDevice(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar dispositivo")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Búsqueda / existencia por UUID (para que el frontend se "autocure")
    // -------------------------------------------------------------------------

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

    @CrossOrigin
    @GetMapping("/medias")
    public ResponseEntity<List<MediaDTO>> getAllMedias() {
        return ResponseEntity.ok(mediaService.getAllMedias());
    }

    // ---------------- UPLOAD ----------------

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
    public ResponseEntity<List<MediaTypeDTO>> getAllMediaTypes() {
        return ResponseEntity.ok(deviceTypeService.getAllMediaTypes());
    }

    @CrossOrigin
    @GetMapping("/medias/types/{id}")
    public ResponseEntity<MediaTypeDTO> getMediaTypeById(@PathVariable Long id) {
        Optional<MediaTypeDTO> device = deviceTypeService.getMediaTypeById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/medias/types")
    public ResponseEntity<MediaTypeDTO> createMediaType(@RequestBody MediaTypeDTO deviceDTO) {
        return ResponseEntity.ok(deviceTypeService.saveMediaType(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/medias/types/{id}")
    public ResponseEntity<MediaTypeDTO> updateMediaType(@PathVariable Long id, @RequestBody MediaTypeDTO deviceDTO) {

        MediaTypeDTO updatedDevice = deviceTypeService.updateMediaType(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);

    }

    @CrossOrigin
    @DeleteMapping("/medias/types/{id}")
    public ResponseEntity<String> deleteMediaType(@PathVariable Long id) {
        deviceTypeService.deleteMediaType(id);
        return ResponseEntity.ok("Media Type (" + id + ") deleted succesfully");
    }
}
```

```java
// src/main/java/com/screenleads/backend/app/web/controller/MetaDataController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.MetadataService;
import com.screenleads.backend.app.web.dto.EntityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/metadata")
// @PreAuthorize("hasAnyRole('admin','company_admin')")
public class MetaDataController {

    private static final Logger log = LoggerFactory.getLogger(MetaDataController.class);

    private final MetadataService metadataService;

    // ✅ Inyección por constructor (sin @Autowired necesario en Spring 4.3+)
    public MetaDataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/entities")
    public ResponseEntity<List<EntityInfo>> getEntities(
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        try {
            return ResponseEntity.ok(metadataService.getAllEntities(withCount));
        } catch (Exception ex) {
            log.error("Error en /metadata/entities", ex);
            return ResponseEntity.ok(List.of()); // nunca tumbes el endpoint
        }
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
    @GetMapping
    public List<PromotionDTO> getAllPromotions() {
        return promotionService.getAllPromotions();
    }

    @GetMapping("/{id}")
    public PromotionDTO getPromotionById(@PathVariable Long id) {
        return promotionService.getPromotionById(id);
    }

    @PostMapping
    public PromotionDTO createPromotion(@RequestBody PromotionDTO promotionDTO) {
        return promotionService.savePromotion(promotionDTO);
    }

    @PutMapping("/{id}")
    public PromotionDTO updatePromotion(@PathVariable Long id, @RequestBody PromotionDTO promotionDTO) {
        return promotionService.updatePromotion(id, promotionDTO);
    }

    @DeleteMapping("/{id}")
    public void deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
    }

    // ===== Leads =====
    @PostMapping("/{id}/leads")
    public PromotionLeadDTO registerLead(@PathVariable Long id, @RequestBody PromotionLeadDTO leadDTO) {
        return promotionService.registerLead(id, leadDTO);
    }

    @GetMapping("/{id}/leads")
    public List<PromotionLeadDTO> listLeads(@PathVariable Long id) {
        return promotionService.listLeads(id);
    }

    // ===== Lead de prueba =====
    @PostMapping("/{id}/leads/test")
    public PromotionLeadDTO createTestLead(
            @PathVariable Long id,
            @RequestBody(required = false) PromotionLeadDTO overrides) {
        return promotionService.createTestLead(id, overrides);
    }

    // ===== Export CSV (Streaming) =====
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
            UserDto created = service.create(dto);
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

