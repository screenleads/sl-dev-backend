// src/main/java/com/screenleads/backend/app/presentation/controller/UserInvitationController.java
package com.screenleads.backend.app.presentation.controller;

import com.screenleads.backend.app.application.dto.CreateInvitationRequest;
import com.screenleads.backend.app.application.dto.UserInvitationDTO;
import com.screenleads.backend.app.application.service.UserInvitationService;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserInvitationController {

    private final UserInvitationService invitationService;
    private final UserRepository userRepository;

    /**
     * Crear nueva invitación de usuario
     * Solo accesible para usuarios con level <= 2
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request,
            Authentication authentication) {
        try {
            String inviterUsername = authentication.getName();
            log.info("Creating invitation for email {} by user {}", request.getEmail(), inviterUsername);
            
            UserInvitationDTO invitation = invitationService.createInvitation(request, inviterUsername);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error creating invitation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal error",
                "message", "Error al crear la invitación"
            ));
        }
    }

    /**
     * Obtener todas las invitaciones de la compañía del usuario actual
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> getCompanyInvitations(Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Fetching invitations for user {}", username);
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            if (user.getCompany() == null) {
                throw new IllegalArgumentException("Usuario sin compañía asignada");
            }
            
            List<UserInvitationDTO> invitations = invitationService.getCompanyInvitations(user.getCompany().getId());
            
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            log.error("Error fetching invitations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal error",
                "message", "Error al obtener las invitaciones"
            ));
        }
    }

    /**
     * Obtener invitación por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> getInvitation(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Fetching invitation {} for user {}", id, username);
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            if (user.getCompany() == null) {
                throw new IllegalArgumentException("Usuario sin compañía asignada");
            }
            
            UserInvitationDTO invitation = invitationService.getCompanyInvitations(user.getCompany().getId())
                    .stream()
                    .filter(inv -> inv.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada"));
            
            return ResponseEntity.ok(invitation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Not found",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error fetching invitation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal error",
                "message", "Error al obtener la invitación"
            ));
        }
    }

    /**
     * Verificar validez de token de invitación (público, sin autenticación)
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestParam String token) {
        try {
            log.info("Verifying invitation token");
            
            UserInvitationDTO invitation = invitationService.verifyToken(token);
            
            // Retornar solo información necesaria para mostrar en el form
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("email", invitation.getEmail());
            response.put("companyName", invitation.getCompanyName());
            response.put("roleName", invitation.getRoleName());
            response.put("inviterName", invitation.getInvitedByName());
            response.put("expiryDate", invitation.getExpiryDate());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid or expired token");
            return ResponseEntity.badRequest().body(Map.of(
                "valid", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error verifying token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal error",
                "message", "Error al verificar el token"
            ));
        }
    }

    /**
     * Reenviar invitación (regenera token y actualiza fecha de expiración)
     */
    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> resendInvitation(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Resending invitation {} by user {}", id, username);
            
            UserInvitationDTO invitation = invitationService.resendInvitation(id, username);
            
            return ResponseEntity.ok(invitation);
        } catch (IllegalArgumentException e) {
            log.error("Validation error resending invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error resending invitation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal error",
                "message", "Error al reenviar la invitación"
            ));
        }
    }

    /**
     * Cancelar invitación
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<?> cancelInvitation(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Cancelling invitation {} by user {}", id, username);
            
            invitationService.cancelInvitation(id, username);
            
            return ResponseEntity.ok(Map.of(
                "message", "Invitación cancelada exitosamente"
            ));
        } catch (IllegalArgumentException e) {
            log.error("Validation error cancelling invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error cancelling invitation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal error",
                "message", "Error al cancelar la invitación"
            ));
        }
    }
}
