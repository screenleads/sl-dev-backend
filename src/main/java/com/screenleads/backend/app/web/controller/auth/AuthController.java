// src/main/java/com/screenleads/backend/app/web/controller/auth/AuthController.java
package com.screenleads.backend.app.web.controller.auth;

import com.screenleads.backend.app.application.security.AuthenticationService;
import com.screenleads.backend.app.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticación y cuentas de usuario")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Devuelve access/refresh token y datos básicos del usuario", security = {})
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PreAuthorize("@authSecurityChecker.allowRegister()")
    @PostMapping("/register")
    @Operation(summary = "Registro de usuario", security = {})
    public ResponseEntity<JwtResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PreAuthorize("@authSecurityChecker.isAuthenticated()")
    @PostMapping("/change-password")
    @Operation(summary = "Cambiar contraseña")
    public ResponseEntity<Void> changePassword(@RequestBody PasswordChangeRequest request) {
        authenticationService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @PreAuthorize("@authSecurityChecker.isAuthenticated()")
    @Operation(summary = "Usuario actual (requiere token)")
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok(authenticationService.getCurrentUser());
    }

    @PreAuthorize("@authSecurityChecker.isAuthenticated()")
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token (requiere token)")
    public ResponseEntity<JwtResponse> refreshTokenProtected() {
        return ResponseEntity.ok(authenticationService.refreshToken());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperación de contraseña", description = "Envía un email con enlace para restablecer contraseña", security = {})
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authenticationService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-reset-token")
    @Operation(summary = "Verificar validez de token de reset", description = "Comprueba si un token es válido, no expirado y no usado", security = {})
    public ResponseEntity<VerifyTokenResponse> verifyResetToken(@RequestParam String token) {
        return ResponseEntity.ok(authenticationService.verifyResetToken(token));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer contraseña", description = "Establece nueva contraseña usando token válido", security = {})
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
