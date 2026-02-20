package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.application.security.jwt.JwtService;
import com.screenleads.backend.app.application.service.EmailService;
import com.screenleads.backend.app.application.service.UserInvitationService;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.PasswordResetToken;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.PasswordResetTokenRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.*;
import com.screenleads.backend.app.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private static final String USER_NOT_FOUND = "Usuario no encontrado";
    private static final String USER_NOT_FOUND_MSG = "User not found";
    private static final int TOKEN_EXPIRY_HOURS = 1;

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final UserInvitationService invitationService;

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
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));

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
        if (auth.getPrincipal() instanceof User userPrincipal) {
            // Always reload from DB to ensure eager fetch
            u = userRepository.findWithCompanyAndProfileImageByUsername(userPrincipal.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
        } else {
            String username = auth.getName();
            u = userRepository.findWithCompanyAndProfileImageByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
        }
        return UserMapper.toDto(u);
    }

    public JwtResponse refreshToken() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new UsernameNotFoundException("No authenticated user");
        }
        User user;
        if (auth.getPrincipal() instanceof User u) {
            user = u;
        } else {
            String username = auth.getName();
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
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
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Solicitar recuperación de contraseña - genera token y envía email
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Buscar usuario por email (case-insensitive)
        String email = request.getEmail().trim();

        // Por seguridad, NO revelamos si el email existe o no
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // Invalidar tokens anteriores del usuario
            passwordResetTokenRepository.markAllUserTokensAsUsed(user);

            // Generar nuevo token único
            String token = UUID.randomUUID().toString();

            // Crear registro de token con expiración de 1 hora
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            // Enviar email con el enlace de reset
            String userName = user.getName() != null ? user.getName() : user.getUsername();
            emailService.sendPasswordResetEmail(user.getEmail(), userName, token);

            log.info("Password reset token generated for user: {}", user.getUsername());
        } else {
            log.warn("Password reset requested for non-existent email: {} (query is case-insensitive)", email);
            // Por seguridad, no hacemos nada pero tampoco lanzamos error
        }
    }

    /**
     * Verificar validez de un token de reset
     */
    @Transactional(readOnly = true)
    public VerifyTokenResponse verifyResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElse(null);

        if (resetToken == null) {
            return VerifyTokenResponse.builder()
                    .valid(false)
                    .message("Token inválido o no encontrado")
                    .build();
        }

        if (resetToken.getUsed()) {
            return VerifyTokenResponse.builder()
                    .valid(false)
                    .message("Este token ya ha sido utilizado")
                    .build();
        }

        if (resetToken.isExpired()) {
            return VerifyTokenResponse.builder()
                    .valid(false)
                    .message("Este token ha expirado")
                    .build();
        }

        return VerifyTokenResponse.builder()
                .valid(true)
                .message("Token válido")
                .userEmail(resetToken.getUser().getEmail())
                .build();
    }

    /**
     * Restablecer contraseña usando token válido
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o no encontrado"));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("El token ha expirado o ya ha sido utilizado");
        }

        User user = resetToken.getUser();

        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Marcar token como usado
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password successfully reset for user: {}", user.getUsername());
    }

    /**
     * Aceptar invitación y crear cuenta de usuario
     */
    @Transactional
    public JwtResponse acceptInvitation(AcceptInvitationRequest request) {
        // Convertir AcceptInvitationRequest a AcceptInvitationRequest del servicio
        com.screenleads.backend.app.application.dto.AcceptInvitationRequest serviceRequest = new com.screenleads.backend.app.application.dto.AcceptInvitationRequest();
        serviceRequest.setToken(request.getToken());
        serviceRequest.setEmail(request.getEmail());
        serviceRequest.setName(request.getName());
        serviceRequest.setLastName(request.getLastName());
        serviceRequest.setUsername(request.getUsername());
        serviceRequest.setPassword(request.getPassword());

        // Aceptar invitación (crea el usuario)
        invitationService.acceptInvitation(serviceRequest);

        // Autenticar al nuevo usuario
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            log.error("Authentication failed for newly created user: {}", request.getUsername(), e);
            throw new RuntimeException("Error en la autenticación del usuario recién creado");
        }

        // Generar y retornar JWT
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MSG));

        String accessToken = jwtService.generateToken(user);

        log.info("User successfully registered via invitation: {}", user.getUsername());

        return JwtResponse.builder()
                .accessToken(accessToken)
                .user(UserMapper.toDto(user))
                .build();
    }
}
