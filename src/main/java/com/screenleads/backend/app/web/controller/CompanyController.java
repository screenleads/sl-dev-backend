// src/main/java/com/screenleads/backend/app/web/controller/CompanyController.java
package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.BillingException;
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

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'write')")
    @PostMapping
    @Operation(summary = "Crear compañía", description = "ROLE_ADMIN o permiso company:write")
    public ResponseEntity<CompanyDTO> createCompany(@RequestBody CompanyDTO companyDTO) {
        return ResponseEntity.ok(companiesService.saveCompany(companyDTO));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'write')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar compañía", description = "ROLE_ADMIN o permiso company:write")
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

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'write')")
    @PostMapping("/{id}/sync-stripe")
    @Operation(summary = "Sincronizar datos de Stripe", description = "Busca el customer en Stripe y sincroniza subscripción")
    public ResponseEntity<?> syncStripeData(@PathVariable Long id) {
        try {
            CompanyDTO updatedCompany = companiesService.syncStripeData(id);
            return ResponseEntity.ok(updatedCompany);
        } catch (BillingException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'write')")
    @PostMapping("/{id}/create-checkout-session")
    @Operation(summary = "Crear sesión de Stripe Checkout", description = "Genera URL de Stripe Checkout para configurar método de pago")
    public ResponseEntity<?> createCheckoutSession(@PathVariable Long id) {
        try {
            String checkoutUrl = companiesService.createCheckoutSession(id);
            return ResponseEntity.ok(new CheckoutResponse(checkoutUrl));
        } catch (BillingException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'write')")
    @PostMapping("/{id}/create-billing-portal")
    @Operation(summary = "Crear sesión del Billing Portal", description = "Genera URL del portal de facturación de Stripe")
    public ResponseEntity<?> createBillingPortal(@PathVariable Long id) {
        try {
            String portalUrl = companiesService.createBillingPortalSession(id);
            return ResponseEntity.ok(new BillingPortalResponse(portalUrl));
        } catch (BillingException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DTOs para respuestas
    private record ErrorResponse(String message) {
    }

    private record CheckoutResponse(String checkoutUrl) {
    }

    private record BillingPortalResponse(String portalUrl) {
    }
}
