package com.screenleads.backend.app.web.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.CompanyBillingService;
import com.screenleads.backend.app.web.dto.CompanyBillingDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin
@RestController
@RequestMapping("/company-billing")
@Tag(name = "Company Billing", description = "Gestión de facturación de empresas")
public class CompanyBillingController {

    private final CompanyBillingService billingService;

    public CompanyBillingController(CompanyBillingService billingService) {
        this.billingService = billingService;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    @Operation(summary = "Listar configuraciones de facturación")
    public ResponseEntity<List<CompanyBillingDTO>> getAllCompanyBillings() {
        return ResponseEntity.ok(billingService.getAllCompanyBillings());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('billing', 'read')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener configuración de facturación por ID")
    public ResponseEntity<CompanyBillingDTO> getCompanyBillingById(@PathVariable Long id) {
        return billingService.getCompanyBillingById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('billing', 'read')")
    @GetMapping("/company/{companyId}")
    @Operation(summary = "Obtener configuración de facturación por empresa")
    public ResponseEntity<CompanyBillingDTO> getCompanyBillingByCompanyId(@PathVariable Long companyId) {
        return billingService.getCompanyBillingByCompanyId(companyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping
    @Operation(summary = "Crear configuración de facturación", description = "Solo ROLE_ADMIN")
    public ResponseEntity<CompanyBillingDTO> createCompanyBilling(@RequestBody CompanyBillingDTO dto) {
        return ResponseEntity.ok(billingService.createCompanyBilling(dto));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar configuración de facturación", description = "Solo ROLE_ADMIN")
    public ResponseEntity<CompanyBillingDTO> updateCompanyBilling(@PathVariable Long id, @RequestBody CompanyBillingDTO dto) {
        return ResponseEntity.ok(billingService.updateCompanyBilling(id, dto));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar configuración de facturación", description = "Solo ROLE_ADMIN")
    public ResponseEntity<Void> deleteCompanyBilling(@PathVariable Long id) {
        billingService.deleteCompanyBilling(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/{id}/reset-period")
    @Operation(summary = "Resetear periodo actual", description = "Solo ROLE_ADMIN")
    public ResponseEntity<CompanyBillingDTO> resetCurrentPeriod(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.resetCurrentPeriod(id));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('billing', 'read')")
    @GetMapping("/{id}/device-limit-reached")
    @Operation(summary = "Verificar si alcanzó límite de devices")
    public ResponseEntity<Boolean> hasReachedDeviceLimit(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.hasReachedDeviceLimit(id));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('billing', 'read')")
    @GetMapping("/{id}/promotion-limit-reached")
    @Operation(summary = "Verificar si alcanzó límite de promociones")
    public ResponseEntity<Boolean> hasReachedPromotionLimit(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.hasReachedPromotionLimit(id));
    }
}
