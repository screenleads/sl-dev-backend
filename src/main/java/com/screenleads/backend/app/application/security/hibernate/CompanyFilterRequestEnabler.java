package com.screenleads.backend.app.application.security.hibernate;

import com.screenleads.backend.app.domain.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(20) // después del JwtAuthenticationFilter
public class CompanyFilterRequestEnabler extends OncePerRequestFilter {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Si no hay autenticación válida o si es admin, no activar filtro
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            boolean isAdmin = user.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

            if (!isAdmin && user.getCompany() != null && user.getCompany().getId() != null) {
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter("companyFilter");
                filter.setParameter("companyId", user.getCompany().getId());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpia el filtro para no “contaminar” otros hilos
            try {
                Session session = entityManager.unwrap(Session.class);
                if (session.getEnabledFilter("companyFilter") != null) {
                    session.disableFilter("companyFilter");
                }
            } catch (Exception ignored) {
            }
        }
    }
}
