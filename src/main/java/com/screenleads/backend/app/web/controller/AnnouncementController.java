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

/**
 * Controlador de Announcements (Anuncios) - Alias para AdvicesController
 * Este controlador existe para mantener compatibilidad con el dashboard que usa /announcements
 * internamente llama al servicio de Advices (advices = announcements)
 */
@Slf4j
@RestController
@RequestMapping("/announcements")
@CrossOrigin(origins = "*")
@Tag(name = "Announcements", description = "Gestión de anuncios (alias de Advices)")
public class AnnouncementController {

    private final AdviceService adviceService;

    public AnnouncementController(AdviceService adviceService) {
        this.adviceService = adviceService;
    }

    @PreAuthorize("@perm.can('advice', 'read')")
    @GetMapping
    @Operation(summary = "Listar todos los anuncios")
    public ResponseEntity<List<AdviceDTO>> getAllAnnouncements() {
        log.info("GET /announcements - Listing all announcements (advices)");
        return ResponseEntity.ok(adviceService.getAllAdvices());
    }

    @PreAuthorize("@perm.can('advice', 'read')")
    @GetMapping("/visibles")
    @Operation(
        summary = "Anuncios visibles ahora",
        description = "Devuelve anuncios activos en la zona horaria del cliente (headers X-Timezone o X-Timezone-Offset)"
    )
    public ResponseEntity<List<AdviceDTO>> getVisibleAnnouncements(
            @Parameter(description = "IANA Timezone, ej: 'Europe/Madrid'") @RequestHeader(value = "X-Timezone", required = false) String tzName,
            @Parameter(description = "Offset en minutos al ESTE de UTC, ej: 120") @RequestHeader(value = "X-Timezone-Offset", required = false) Integer offsetMinutes) {
        
        log.info("GET /announcements/visibles - timezone={}, offset={}", tzName, offsetMinutes);
        
        ZoneId zoneId = null;
        if (tzName != null && !tzName.isBlank()) {
            try {
                zoneId = ZoneId.of(tzName);
            } catch (Exception e) {
                log.warn("Invalid timezone: {}", tzName, e);
            }
        }
        if (zoneId == null && offsetMinutes != null) {
            zoneId = ZoneOffset.ofTotalSeconds(offsetMinutes * 60);
        }
        if (zoneId == null) {
            zoneId = ZoneId.systemDefault();
        }
        
        return ResponseEntity.ok(adviceService.getVisibleAdvices(zoneId));
    }

    @PreAuthorize("@perm.can('advice', 'read')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener anuncio por ID")
    public ResponseEntity<AdviceDTO> getAnnouncementById(@PathVariable Long id) {
        log.info("GET /announcements/{} - Getting announcement", id);
        Optional<AdviceDTO> advice = adviceService.getAdviceById(id);
        return advice.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("@perm.can('advice', 'write')")
    @PostMapping
    @Operation(summary = "Crear nuevo anuncio")
    public ResponseEntity<AdviceDTO> createAnnouncement(@RequestBody AdviceDTO dto) {
        log.info("POST /announcements - Creating announcement");
        return ResponseEntity.ok(adviceService.saveAdvice(dto));
    }

    @PreAuthorize("@perm.can('advice', 'write')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar anuncio")
    public ResponseEntity<AdviceDTO> updateAnnouncement(@PathVariable Long id, @RequestBody AdviceDTO dto) {
        log.info("PUT /announcements/{} - Updating announcement", id);
        return ResponseEntity.ok(adviceService.updateAdvice(id, dto));
    }

    @PreAuthorize("@perm.can('advice', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar anuncio")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        log.info("DELETE /announcements/{} - Deleting announcement", id);
        adviceService.deleteAdvice(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@perm.can('advice', 'read')")
    @GetMapping("/company/{companyId}")
    @Operation(summary = "Obtener anuncios por compañía")
    public ResponseEntity<List<AdviceDTO>> getAnnouncementsByCompany(@PathVariable Long companyId) {
        log.info("GET /announcements/company/{} - Getting announcements by company", companyId);
        return ResponseEntity.ok(adviceService.getAdvicesByCompany(companyId));
    }

    @PreAuthorize("@perm.can('advice', 'read')")
    @GetMapping("/client/{clientId}")
    @Operation(summary = "Obtener anuncios por cliente publicitario")
    public ResponseEntity<List<AdviceDTO>> getAnnouncementsByClient(@PathVariable Long clientId) {
        log.info("GET /announcements/client/{} - Getting announcements by client", clientId);
        return ResponseEntity.ok(adviceService.getAdvicesByClient(clientId));
    }
}
