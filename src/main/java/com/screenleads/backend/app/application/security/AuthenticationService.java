package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.application.security.jwt.JwtService;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.JwtResponse;
import com.screenleads.backend.app.web.dto.LoginRequest;
import com.screenleads.backend.app.web.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
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
        System.out.println("Usuarios registrados?: " + userRepository.count());

        if (userRepository.count() == 0) {
            // Primer usuario: asignamos el rol ADMIN
            Role adminRole = roleRepository.findByRole("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Role ROLE_ADMIN not found"));
            user.setRoles(Set.of(adminRole));
        } else {
            // Por defecto podrías asignar un rol básico si quieres
            Role defaultRole = roleRepository.findByRole("ROLE_COMPANY_VIEWER")
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setRoles(Set.of(defaultRole));
        }

        userRepository.save(user);
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