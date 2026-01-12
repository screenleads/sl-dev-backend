// src/main/java/com/screenleads/backend/app/web/controller/AppVersionController.java
package com.screenleads.backend.app.web.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Eliminar versión por id")
    public void deleteById(@PathVariable Long id) {
        service.deleteById(id);
    }

    @GetMapping("/latest/{platform}")
    @Operation(summary = "Última versión por plataforma")
    public ResponseEntity<AppVersionDTO> getLatestVersion(@PathVariable String platform) {
        AppVersionDTO version = service.getLatestVersion(platform);
        if (version == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(version);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar versión por id")
    public AppVersionDTO update(@PathVariable Long id, @RequestBody AppVersionDTO dto) {
        dto.setId(id);
        return service.save(dto);
    }
}
