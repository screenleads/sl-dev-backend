// src/main/java/com/screenleads/backend/app/web/controller/AdvicesController.java
package com.screenleads.backend.app.web.controller;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.AdviceService;
import com.screenleads.backend.app.web.dto.AdviceDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/advices")
@Tag(name = "Advices", description = "Gestión y consulta de anuncios (advices)")
public class AdvicesController {

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
        log.debug("Resolviendo visibles con zona: {}", zone);
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
        log.info("adviceDTO object: {}", adviceDTO);
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
                log.warn("X-Timezone inválida '{}': {}", tz, e.getMessage());
            }
        }
        if (offsetMinutesStr != null && !offsetMinutesStr.isBlank()) {
            try {
                int minutes = Integer.parseInt(offsetMinutesStr.trim());
                return ZoneOffset.ofTotalSeconds(minutes * 60);
            } catch (Exception e) {
                log.warn("X-Timezone-Offset inválido '{}': {}", offsetMinutesStr, e.getMessage());
            }
        }
        return ZoneId.systemDefault();
    }
}
