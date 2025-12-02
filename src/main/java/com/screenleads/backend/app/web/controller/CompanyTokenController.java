package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.domain.model.CompanyToken;
import com.screenleads.backend.app.application.service.CompanyTokenService;
import com.screenleads.backend.app.web.dto.CompanyTokenDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/company-tokens")
public class CompanyTokenController {
            @PutMapping("/{id}")
            @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('companytoken', 'update')")
            public ResponseEntity<CompanyTokenDTO> updateToken(@PathVariable Long id, @RequestBody CompanyTokenDTO dto) {
                Optional<CompanyToken> updated = companyTokenService.updateToken(id, dto);
                return updated.map(t -> ResponseEntity.ok(toDto(t))).orElseGet(() -> ResponseEntity.notFound().build());
            }
        @GetMapping("/{id}")
        @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('companytoken', 'read')")
        public ResponseEntity<CompanyTokenDTO> getTokenById(@PathVariable Long id) {
            Optional<CompanyToken> token = companyTokenService.getTokenById(id);
            return token.map(t -> ResponseEntity.ok(toDto(t))).orElseGet(() -> ResponseEntity.notFound().build());
        }
    private final CompanyTokenService companyTokenService;

    public CompanyTokenController(CompanyTokenService companyTokenService) {
        this.companyTokenService = companyTokenService;
    }



    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('companytoken', 'create')")
    public ResponseEntity<CompanyTokenDTO> createToken(@AuthenticationPrincipal UserDetails userDetails, @RequestBody(required = false) CompanyTokenDTO dto) {
        CompanyToken token = companyTokenService.createTokenForUser(userDetails.getUsername(), dto != null ? dto.getDescripcion() : null);
        return ResponseEntity.ok(toDto(token));
    }



    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('companytoken', 'read')")
    public ResponseEntity<List<CompanyTokenDTO>> getTokens(@AuthenticationPrincipal UserDetails userDetails) {
        List<CompanyToken> tokens = companyTokenService.getTokensForAuthenticatedUser(userDetails.getUsername());
        List<CompanyTokenDTO> dtos = tokens.stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }



    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('companytoken', 'delete')")
    public ResponseEntity<Void> deleteToken(@PathVariable Long id) {
        companyTokenService.deleteToken(id);
        return ResponseEntity.noContent().build();
    }



    @PutMapping("/{token}/renew")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('companytoken', 'update')")
    public ResponseEntity<CompanyTokenDTO> renewToken(@PathVariable String token) {
        Optional<CompanyToken> renewed = companyTokenService.renewToken(token);
        return renewed.map(t -> ResponseEntity.ok(toDto(t))).orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PutMapping("/{token}/description")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('companytoken', 'update')")
    public ResponseEntity<CompanyTokenDTO> updateDescription(@PathVariable String token, @RequestBody CompanyTokenDTO dto) {
        Optional<CompanyToken> updated = companyTokenService.updateDescription(token, dto.getDescripcion());
        return updated.map(t -> ResponseEntity.ok(toDto(t))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private CompanyTokenDTO toDto(CompanyToken token) {
        return CompanyTokenDTO.builder()
                .id(token.getId())
                .companyId(token.getCompanyId())
                .token(token.getToken())
                .role(token.getRole())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .descripcion(token.getDescripcion())
                .build();
    }
}
