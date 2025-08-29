package com.screenleads.backend.app.application.security.hibernate;

import com.screenleads.backend.app.domain.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(20)
public class CompanyFilterRequestEnabler extends OncePerRequestFilter {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    public CompanyFilterRequestEnabler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()) {
                boolean isAdmin = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));

                Long companyId = resolveCompanyId(auth);
                if (!isAdmin && companyId != null) {
                    Session session = entityManager.unwrap(Session.class);
                    var filter = session.enableFilter("companyFilter");
                    filter.setParameter("companyId", companyId);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            try {
                Session s = entityManager.unwrap(Session.class);
                if (s.getEnabledFilter("companyFilter") != null)
                    s.disableFilter("companyFilter");
            } catch (Exception ignored) {
            }
        }
    }

    private Long resolveCompanyId(Authentication auth) {
        Object principal = auth.getPrincipal();

        // 1) Tu entidad de dominio como principal
        if (principal instanceof com.screenleads.backend.app.domain.model.User u) {
            return u.getCompany() != null ? u.getCompany().getId() : null;
        }

        // 2) UserDetails estándar
        if (principal instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        // 3) Principal como String (username)
        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        return null;
    }
}
