package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.screenleads.backend.app.application.service.CompaniesService;
import com.screenleads.backend.app.web.dto.CompanyDTO;

@Controller
public class CompanyController {
    @Autowired
    private CompaniesService CompaniesService;

    public CompanyController(CompaniesService CompaniesService) {
        this.CompaniesService = CompaniesService;
    }

    @CrossOrigin
    @GetMapping("/companies")
    public ResponseEntity<List<CompanyDTO>> getAllCompanies() {
        return ResponseEntity.ok(CompaniesService.getAllCompanies());
    }

    @CrossOrigin
    @GetMapping("/companies/{id}")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long id) {
        Optional<CompanyDTO> company = CompaniesService.getCompanyById(id);
        return company.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/companies")
    public ResponseEntity<CompanyDTO> createCompany(@RequestBody CompanyDTO companyDTO) {
        return ResponseEntity.ok(CompaniesService.saveCompany(companyDTO));
    }

    @CrossOrigin
    @PutMapping("/companies/{id}")
    public ResponseEntity<CompanyDTO> updateCompany(@PathVariable Long id, @RequestBody CompanyDTO companyDTO) {
        try {
            CompanyDTO updatedCompany = CompaniesService.updateCompany(id, companyDTO);
            return ResponseEntity.ok(updatedCompany);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @CrossOrigin
    @DeleteMapping("/companies/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        CompaniesService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}