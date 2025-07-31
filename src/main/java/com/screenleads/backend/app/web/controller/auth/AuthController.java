package com.screenleads.backend.app.web.controller.auth;

import com.screenleads.backend.app.application.security.AuthenticationService;
import com.screenleads.backend.app.web.dto.JwtResponse;
import com.screenleads.backend.app.web.dto.LoginRequest;
import com.screenleads.backend.app.web.dto.PasswordChangeRequest;
import com.screenleads.backend.app.web.dto.RegisterRequest;
import com.screenleads.backend.app.web.dto.UserDto;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PreAuthorize("@authSecurityChecker.allowRegister()")
    @PostMapping("/register")
    public ResponseEntity<JwtResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PreAuthorize("@authSecurityChecker.isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody PasswordChangeRequest request) {
        authenticationService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUser() {
        return ResponseEntity.ok(authenticationService.getCurrentUser());
    }

}