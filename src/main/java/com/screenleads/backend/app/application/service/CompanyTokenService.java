package com.screenleads.backend.app.application.service;
import com.screenleads.backend.app.domain.repositories.UserRepository;

import com.screenleads.backend.app.domain.model.CompanyToken;
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
    private final CompanyTokenRepository companyTokenRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public CompanyTokenService(CompanyTokenRepository companyTokenRepository, JwtService jwtService, UserRepository userRepository) {
        this.companyTokenRepository = companyTokenRepository;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }
    public CompanyToken createTokenForUser(String username) {
        return userRepository.findByUsername(username)
            .map(user -> createToken(user.getCompany().getId()))
            .orElseThrow(() -> new IllegalArgumentException("User not found or has no company"));
    }

    public CompanyToken createToken(Long companyId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusYears(1);
        String role = "company_admin";
        String token = Jwts.builder()
            .setSubject(companyId.toString())
            .claim("role", role)
            .setIssuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
            .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
            .signWith(jwtService.getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
        CompanyToken companyToken = new CompanyToken();
        companyToken.setCompanyId(companyId);
        companyToken.setToken(token);
        companyToken.setRole(role);
        companyToken.setCreatedAt(now);
        companyToken.setExpiresAt(expiresAt);
        return companyTokenRepository.save(companyToken);
    }

    public List<CompanyToken> getTokensByCompany(Long companyId) {
        return companyTokenRepository.findByCompanyId(companyId);
    }

    @Transactional
    public void deleteToken(String token) {
        companyTokenRepository.deleteByToken(token);
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