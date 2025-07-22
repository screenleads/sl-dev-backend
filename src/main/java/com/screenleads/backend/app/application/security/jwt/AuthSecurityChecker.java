package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authSecurityChecker")
@RequiredArgsConstructor
public class AuthSecurityChecker {

    private final UserRepository userRepository;

    public boolean allowRegister() {
        // Si no hay usuarios, permitir el registro libre
        if (userRepository.count() == 0) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Si no hay autenticación válida o no tiene roles, denegar
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities() == null) {
            return false;
        }

        // Permitir solo si tiene ROLE_ADMIN
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
