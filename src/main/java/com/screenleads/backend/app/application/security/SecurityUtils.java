package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.domain.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public static boolean isAdmin() {
        return getCurrentUser()
                .map(u -> u.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())))
                .orElse(false);
    }

    public static Optional<Long> currentCompanyId() {
        return getCurrentUser().map(u -> u.getCompany() != null ? u.getCompany().getId() : null);
    }
}
