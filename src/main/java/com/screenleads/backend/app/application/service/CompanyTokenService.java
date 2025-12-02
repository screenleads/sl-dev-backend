// ...eliminado método fuera de clase...
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.domain.model.User;

import com.screenleads.backend.app.domain.model.CompanyToken;
import com.screenleads.backend.app.web.dto.CompanyTokenDTO;
import com.screenleads.backend.app.domain.repositories.CompanyTokenRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import com.screenleads.backend.app.application.security.jwt.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CompanyTokenService {
    @Transactional
    public Optional<CompanyToken> updateToken(Long id, CompanyTokenDTO dto) {
        Optional<CompanyToken> optToken = companyTokenRepository.findById(id);
        if (optToken.isEmpty())
            return Optional.empty();
        CompanyToken token = optToken.get();
        // Actualiza los campos permitidos
        if (dto.getDescripcion() != null)
            token.setDescripcion(dto.getDescripcion());
        if (dto.getCompanyId() != null)
            token.setCompanyId(dto.getCompanyId());
        if (dto.getToken() != null)
            token.setToken(dto.getToken());
        if (dto.getRole() != null)
            token.setRole(dto.getRole());
        if (dto.getExpiresAt() != null)
            token.setExpiresAt(dto.getExpiresAt());
        // No se actualiza id ni createdAt
        companyTokenRepository.save(token);
        return Optional.of(token);
    }

    public Optional<CompanyToken> getTokenById(Long id) {
        return companyTokenRepository.findById(id);
    }

    public List<CompanyToken> getTokensForAuthenticatedUser(String username) {
        return userRepository.findByUsername(username)
                .map(user -> getTokensByCompany(user.getCompany().getId()))
                .orElse(List.of());
    }

    private final CompanyTokenRepository companyTokenRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public CompanyTokenService(CompanyTokenRepository companyTokenRepository, JwtService jwtService,
            UserRepository userRepository) {
        this.companyTokenRepository = companyTokenRepository;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public CompanyToken createTokenForUser(String username, String descripcion) {
        return userRepository.findByUsername(username)
                .map(user -> createToken(user, descripcion))
                .orElseThrow(() -> new IllegalArgumentException("User not found or has no company"));
    }

    public CompanyToken createToken(User user, String descripcion) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusYears(1);
        String role = "company_admin";
        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", role)
                .setIssuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(jwtService.getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        CompanyToken companyToken = new CompanyToken();
        companyToken.setCompanyId(user.getCompany().getId());
        companyToken.setToken(token);
        companyToken.setRole(role);
        companyToken.setCreatedAt(now);
        companyToken.setExpiresAt(expiresAt);
        companyToken.setDescripcion(descripcion);
        return companyTokenRepository.save(companyToken);
    }

    public Optional<CompanyToken> updateDescription(String token, String descripcion) {
        CompanyToken companyToken = companyTokenRepository.findByToken(token);
        if (companyToken == null)
            return Optional.empty();
        companyToken.setDescripcion(descripcion);
        companyTokenRepository.save(companyToken);
        return Optional.of(companyToken);
    }

    public List<CompanyToken> getTokensByCompany(Long companyId) {
        return companyTokenRepository.findByCompanyId(companyId);
    }

    @Transactional
    public void deleteToken(Long id) {
        companyTokenRepository.deleteById(id);
    }

    public Optional<CompanyToken> renewToken(String token) {
        CompanyToken companyToken = companyTokenRepository.findByToken(token);
        if (companyToken == null)
            return Optional.empty();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusYears(1);
        String newToken = Jwts.builder()
                .setSubject(companyToken.getCompanyId().toString())
                .claim("role", companyToken.getRole())
                .setIssuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(jwtService.getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        companyToken.setToken(newToken);
        companyToken.setCreatedAt(now);
        companyToken.setExpiresAt(expiresAt);
        companyTokenRepository.save(companyToken);
        return Optional.of(companyToken);
    }
}