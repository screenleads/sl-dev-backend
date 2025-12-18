package com.screenleads.backend.app.application.security;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtro que habilita automáticamente el filtro de Hibernate para restringir
 * las consultas a una compañía específica cuando una API Key con alcance
 * restringido está autenticada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyCompanyFilterEnabler extends OncePerRequestFilter {

    private final EntityManager entityManager;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()) {
                // Verificar si es una autenticación de API_CLIENT
                boolean isApiClient = auth.getAuthorities().stream()
                        .anyMatch(a -> "API_CLIENT".equals(a.getAuthority()));

                if (isApiClient) {
                    Object principal = auth.getPrincipal();

                    if (principal instanceof ApiKeyPrincipal apiKeyPrincipal) {

                        // Si tiene alcance restringido a una compañía, habilitar el filtro
                        if (apiKeyPrincipal.hasRestrictedAccess()) {
                            Long companyId = apiKeyPrincipal.getCompanyScope();
                            enableCompanyFilter(companyId);
                            log.debug("Filtro de compañía habilitado para API Key. CompanyId: {}", companyId);
                        } else {
                            log.debug("API Key con acceso global. No se aplica filtro de compañía.");
                        }
                    }
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            // Limpiar filtros de Hibernate al terminar la petición
            disableCompanyFilter();
        }
    }

    /**
     * Habilita el filtro de Hibernate para la compañía específica.
     */
    private void enableCompanyFilter(Long companyId) {
        try {
            Session session = entityManager.unwrap(Session.class);
            org.hibernate.Filter filter = session.enableFilter("companyFilter");
            filter.setParameter("companyId", companyId);
            log.trace("Filtro Hibernate 'companyFilter' habilitado con companyId={}", companyId);
        } catch (Exception e) {
            log.warn("No se pudo habilitar el filtro de compañía", e);
        }
    }

    /**
     * Deshabilita el filtro de Hibernate.
     */
    private void disableCompanyFilter() {
        try {
            Session session = entityManager.unwrap(Session.class);
            session.disableFilter("companyFilter");
            log.trace("Filtro Hibernate 'companyFilter' deshabilitado");
        } catch (Exception e) {
            log.trace("Error deshabilitando filtro de compañía (puede ser normal si no estaba habilitado)", e);
        }
    }
}
