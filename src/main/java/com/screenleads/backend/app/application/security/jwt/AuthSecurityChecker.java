package com.screenleads.backend.app.application.security.jwt;

import com.screenleads.backend.app.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthSecurityChecker {

    private final UserRepository userRepository;

    public boolean allowRegister() {
        // Permitir si no hay usuarios
        if (userRepository.count() == 0) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Verificar autenticación válida
        if (auth == null || !auth.isAuthenticated()) {
            System.out.println("Authenticated: " + auth.isAuthenticated());
            System.out.println("Authorities: " + auth.getAuthorities());
            System.out.println("User count: " + userRepository.count());
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
