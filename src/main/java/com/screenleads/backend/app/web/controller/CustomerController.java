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
    @PreAuthorize("@perm.can('customer', 'write')")
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
    @PreAuthorize("@perm.can('customer', 'write')")
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
