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

    private static final String ERROR_INTEGRITY = "Violación de integridad (¿username/email único?)";
    private static final String ERROR_CREATE_USER = "No se pudo crear el usuario";
    private static final String ERROR_UPDATE_USER = "No se pudo actualizar el usuario";

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
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ERROR_INTEGRITY));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ERROR_CREATE_USER));
        }
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('user','update')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UserDto dto) {
        try {
            UserDto updated = service.update(id, dto);
            if (updated != null) {
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ERROR_INTEGRITY));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ERROR_UPDATE_USER));
        }
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('user','delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
