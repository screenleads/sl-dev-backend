package com.screenleads.backend.app.application.service.impl;

import com.screenleads.backend.app.application.dto.AcceptInvitationRequest;
import com.screenleads.backend.app.application.dto.CreateInvitationRequest;
import com.screenleads.backend.app.application.dto.UserInvitationDTO;
import com.screenleads.backend.app.application.service.EmailService;
import com.screenleads.backend.app.application.service.UserInvitationService;
import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInvitationServiceImpl implements UserInvitationService {

    private final UserInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    @Transactional
    public UserInvitationDTO createInvitation(CreateInvitationRequest request, String inviterUsername) {
        log.info("Creating invitation for email: {}", request.getEmail());
        
        // 1. Obtener usuario invitador
        User inviter = userRepository.findByUsername(inviterUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario invitador no encontrado"));
        
        if (inviter.getCompany() == null) {
            throw new IllegalArgumentException("El usuario debe pertenecer a una empresa");
        }
        
        // 2. Verificar que el email no exista ya como usuario
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con este email");
        }
        
        // 3. Verificar que no haya invitación pendiente para este email
        boolean hasPendingInvitation = invitationRepository.existsByEmailAndStatusIn(
                request.getEmail(), 
                Arrays.asList(InvitationStatus.PENDING)
        );
        if (hasPendingInvitation) {
            throw new IllegalArgumentException("Ya existe una invitación pendiente para este email");
        }
        
        // 4. Obtener rol y validar permisos
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));
        
        // Validar que el invitador pueda asignar este rol (nivel del rol >= nivel del invitador)
        if (role.getLevel() < inviter.getRole().getLevel()) {
            throw new IllegalArgumentException(
                    String.format("No puedes asignar un rol de nivel %d cuando tu nivel es %d", 
                            role.getLevel(), inviter.getRole().getLevel())
            );
        }
        
        // 5. Generar token único
        String token = UUID.randomUUID().toString();
        
        // 6. Crear invitación
        UserInvitation invitation = UserInvitation.builder()
                .email(request.getEmail())
                .invitedBy(inviter)
                .company(inviter.getCompany())
                .role(role)
                .token(token)
                .customMessage(request.getCustomMessage())
                .status(InvitationStatus.PENDING)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .sentAt(LocalDateTime.now())
                .build();
        
        invitation = invitationRepository.save(invitation);
        
        // 7. Enviar email
        try {
            sendInvitationEmail(invitation);
            log.info("Invitation email sent successfully to {}", request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send invitation email to {}", request.getEmail(), e);
            // No lanzamos excepción, la invitación se creó correctamente
        }
        
        return toDTO(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserInvitationDTO> getCompanyInvitations(Long companyId) {
        return invitationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserInvitationDTO getInvitationById(Long id) {
        UserInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada"));
        return toDTO(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInvitationDTO verifyToken(String token) {
        UserInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de invitación inválido"));
        
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Esta invitación ya no está activa");
        }
        
        if (invitation.isExpired()) {
            throw new IllegalArgumentException("Esta invitación ha expirado");
        }
        
        return toDTO(invitation);
    }

    @Override
    @Transactional
    public User acceptInvitation(AcceptInvitationRequest request) {
        log.info("Accepting invitation with token for email: {}", request.getEmail());
        
        // 1. Verificar token y obtener invitación
        UserInvitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token de invitación inválido"));
        
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Esta invitación ya no está activa");
        }
        
        if (invitation.isExpired()) {
            throw new IllegalArgumentException("Esta invitación ha expirado");
        }
        
        // 2. Validar que el email coincida con la invitación
        if (!invitation.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Debes registrarte con el email de la invitación: " + invitation.getEmail()
            );
        }
        
        // 3. Verificar que el usuario no exista
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con este email");
        }
        
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con este username");
        }
        
        // 4. Crear nuevo usuario
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .name(request.getName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(request.getPassword()))
                .company(invitation.getCompany())
                .role(invitation.getRole())
                .build();
        
        newUser = userRepository.save(newUser);
        
        // 5. Marcar invitación como aceptada
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
        
        log.info("User created successfully from invitation: {}", newUser.getUsername());
        
        return newUser;
    }

    @Override
    @Transactional
    public void cancelInvitation(Long id, String username) {
        UserInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada"));
        
        // Verificar que el usuario pertenece a la misma empresa
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        if (!invitation.getCompany().getId().equals(user.getCompany().getId())) {
            throw new IllegalArgumentException("No tienes permisos para cancelar esta invitación");
        }
        
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Solo se pueden cancelar invitaciones pendientes");
        }
        
        invitation.setStatus(InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);
        
        log.info("Invitation {} cancelled by user {}", id, username);
    }

    @Override
    @Transactional
    public UserInvitationDTO resendInvitation(Long id, String username) {
        UserInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada"));
        
        // Verificar permisos
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        if (!invitation.getCompany().getId().equals(user.getCompany().getId())) {
            throw new IllegalArgumentException("No tienes permisos para reenviar esta invitación");
        }
        
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Solo se pueden reenviar invitaciones pendientes");
        }
        
        // Actualizar fecha de expiración y reenviar
        invitation.setExpiryDate(LocalDateTime.now().plusDays(7));
        invitation.setSentAt(LocalDateTime.now());
        invitation = invitationRepository.save(invitation);
        
        // Reenviar email
        try {
            sendInvitationEmail(invitation);
            log.info("Invitation email resent successfully to {}", invitation.getEmail());
        } catch (Exception e) {
            log.error("Failed to resend invitation email to {}", invitation.getEmail(), e);
        }
        
        return toDTO(invitation);
    }

    @Override
    @Transactional
    public void expireOldInvitations() {
        int expiredCount = invitationRepository.markExpiredInvitations(LocalDateTime.now());
        if (expiredCount > 0) {
            log.info("Marked {} invitations as expired", expiredCount);
        }
    }

    private void sendInvitationEmail(UserInvitation invitation) {
        String inviterName = invitation.getInvitedBy().getName() != null 
                ? invitation.getInvitedBy().getName() 
                : invitation.getInvitedBy().getUsername();
        
        String companyName = invitation.getCompany().getName();
        String roleName = invitation.getRole().getRole();
        String token = invitation.getToken();
        String email = invitation.getEmail();
        
        // Aquí usarías el EmailService para enviar el email con plantilla
        emailService.sendUserInvitationEmail(
                email, 
                inviterName, 
                companyName, 
                roleName, 
                token, 
                invitation.getCustomMessage()
        );
    }

    private UserInvitationDTO toDTO(UserInvitation invitation) {
        return UserInvitationDTO.builder()
                .id(invitation.getId())
                .email(invitation.getEmail())
                .invitedByUserId(invitation.getInvitedBy().getId())
                .invitedByName(invitation.getInvitedBy().getName())
                .companyId(invitation.getCompany().getId())
                .companyName(invitation.getCompany().getName())
                .roleId(invitation.getRole().getId())
                .roleName(invitation.getRole().getRole())
                .roleLevel(invitation.getRole().getLevel())
                .token(invitation.getToken())
                .customMessage(invitation.getCustomMessage())
                .status(invitation.getStatus())
                .expiryDate(invitation.getExpiryDate())
                .createdAt(invitation.getCreatedAt())
                .sentAt(invitation.getSentAt())
                .acceptedAt(invitation.getAcceptedAt())
                .expired(invitation.isExpired())
                .build();
    }
}
