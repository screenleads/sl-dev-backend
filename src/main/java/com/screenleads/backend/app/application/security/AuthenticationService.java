package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.application.security.jwt.JwtService;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.JwtResponse;
import com.screenleads.backend.app.web.dto.LoginRequest;
import com.screenleads.backend.app.web.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;


@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public JwtResponse register(RegisterRequest request) {
    User user = new User();
    user.setEmail(request.getEmail());
    user.setUsername(request.getUsername());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setName(request.getName());
    user.setLastName(request.getLastName());

    // Setea la compañía si viene en la request
    if (request.getCompanyId() != null) {
        Company company = companyRepository.findById(request.getCompanyId())
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId()));
        user.setCompany(company);
    }

    if (userRepository.count() == 0) {
        Role adminRole = roleRepository.findByRole("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role ROLE_ADMIN not found"));
        user.setRoles(Set.of(adminRole));
    } else {
        Role defaultRole = roleRepository.findByRole("ROLE_COMPANY_VIEWER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.setRoles(Set.of(defaultRole));
    }

    try {
        userRepository.save(user);
    } catch (DataIntegrityViolationException ex) {
        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof SQLException sqlEx) {
            String message = sqlEx.getMessage();
            if (message != null && message.toLowerCase().contains("duplicate")) {
                throw new RuntimeException("El nombre de usuario o email ya está registrado.");
            } else {
                throw new RuntimeException("Error de integridad de datos: " + message);
            }
        }
        throw new RuntimeException("Error inesperado al registrar usuario.");
    }
    return new JwtResponse(jwtService.generateToken(user), user);
}

    public JwtResponse login(LoginRequest request) throws AuthenticationException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new JwtResponse(jwtService.generateToken(user), user);
    }
}