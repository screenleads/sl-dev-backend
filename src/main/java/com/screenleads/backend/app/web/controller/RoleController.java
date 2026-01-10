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
    @PreAuthorize("@perm.can('user','write')")
    public ResponseEntity<RoleDTO> create(@RequestBody RoleDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('user','write')")
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
    @PreAuthorize("@perm.can('user','write')")
    public ResponseEntity<List<RoleDTO>> assignable() {
        int myLevel = perm.effectiveLevel();
        List<RoleDTO> all = service.getAll();
        List<RoleDTO> allowed = all.stream()
                .filter(r -> r.level() != null && r.level() >= myLevel)
                .toList();
        return ResponseEntity.ok(allowed);
    }
}
