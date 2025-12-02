// src/main/java/com/screenleads/backend/app/web/controller/CompanyController.java
package com.screenleads.backend.app.web.controller;

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

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'create')")
    @PostMapping
    @Operation(summary = "Crear compañía", description = "ROLE_ADMIN o permiso company:create")
    public ResponseEntity<CompanyDTO> createCompany(@RequestBody CompanyDTO companyDTO) {
        return ResponseEntity.ok(companiesService.saveCompany(companyDTO));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'update')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar compañía", description = "ROLE_ADMIN o permiso company:update")
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
}
