package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.domain.model.CompanyToken;
import com.screenleads.backend.app.application.service.CompanyTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/company-tokens")
public class CompanyTokenController {
    private final CompanyTokenService companyTokenService;

    public CompanyTokenController(CompanyTokenService companyTokenService) {
        this.companyTokenService = companyTokenService;
    }

    @PostMapping("/create")
    public ResponseEntity<CompanyToken> createToken(@AuthenticationPrincipal UserDetails userDetails) {
        CompanyToken token = companyTokenService.createTokenForUser(userDetails.getUsername());
        return ResponseEntity.ok(token);
    }

    @GetMapping("/list/{companyId}")
    public ResponseEntity<List<CompanyToken>> getTokens(@PathVariable Long companyId) {
        List<CompanyToken> tokens = companyTokenService.getTokensByCompany(companyId);
        return ResponseEntity.ok(tokens);
    }

    @DeleteMapping("/delete/{token}")
    public ResponseEntity<Void> deleteToken(@PathVariable String token) {
        companyTokenService.deleteToken(token);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/renew/{token}")
    public ResponseEntity<CompanyToken> renewToken(@PathVariable String token) {
        Optional<CompanyToken> renewed = companyTokenService.renewToken(token);
        return renewed.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
