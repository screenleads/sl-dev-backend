package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.PermissionService;
import com.screenleads.backend.app.application.service.RoleService;
import com.screenleads.backend.app.web.dto.RoleDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
// @PreAuthorize("hasAnyRole('admin','company_admin')") // opcional si aún lo
// usas
public class RoleController {

    private final RoleService service;
    private final PermissionService perm;

    public RoleController(RoleService service, PermissionService perm) {
        this.service = service;
        this.perm = perm;
    }

    @GetMapping
    @PreAuthorize("@perm.can('user','read')") // lectura de roles suele requerir permiso de gestión de usuarios
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
    @PreAuthorize("@perm.can('user','update')") // o define permisos específicos de role si lo prefieres
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
     * Devuelve SOLO los roles asignables por el usuario actual:
     * - requiere permiso para crear o actualizar usuarios
     * - filtra por jerarquía: level >= level efectivo del solicitante
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
