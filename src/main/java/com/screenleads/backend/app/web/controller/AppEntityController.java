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
