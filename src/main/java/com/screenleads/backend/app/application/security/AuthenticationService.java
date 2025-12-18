package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.application.security.jwt.JwtService;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.*;
import com.screenleads.backend.app.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
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

        if (request.getCompanyId() != null) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId()));
            user.setCompany(company);
        }

        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByRole("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Role ROLE_ADMIN not found"));
            user.setRole(adminRole);
        } else {
            Role defaultRole = roleRepository.findByRole("ROLE_COMPANY_VIEWER")
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            user.setRole(defaultRole);
        }

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return JwtResponse.builder()
                .accessToken(token)
                .user(UserMapper.toDto(user)) // <<< DTO, no entidad
                .build();
    }

    public JwtResponse login(LoginRequest request) throws AuthenticationException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = jwtService.generateToken(user);
        return JwtResponse.builder()
                .accessToken(token)
                .user(UserMapper.toDto(user)) // <<< DTO, no entidad
                .build();
    }

    @Transactional
    public UserDto getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new UsernameNotFoundException("No authenticated user");
        }
        User u;
        if (auth.getPrincipal() instanceof User) {
            User userPrincipal = (User) auth.getPrincipal();
            // Always reload from DB to ensure eager fetch
            u = userRepository.findWithCompanyAndProfileImageByUsername(userPrincipal.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        } else {
            String username = auth.getName();
            u = userRepository.findWithCompanyAndProfileImageByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        }
        return UserMapper.toDto(u);
    }

    public JwtResponse refreshToken() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new UsernameNotFoundException("No authenticated user");
        }
        User user;
        if (auth.getPrincipal() instanceof User) {
            user = (User) auth.getPrincipal();
        } else {
            String username = auth.getName();
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        }
        String token = jwtService.generateToken(user);
        return JwtResponse.builder()
                .accessToken(token)
                .user(UserMapper.toDto(user))
                .build();
    }

    public void changePassword(PasswordChangeRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
