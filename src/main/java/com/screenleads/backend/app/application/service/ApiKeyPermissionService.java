package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("apiKeyPerm")
public class ApiKeyPermissionService {
    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyPermissionService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public boolean can(String resource, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (!"API_CLIENT".equals(auth.getAuthorities().stream().findFirst().map(Object::toString).orElse(null))) return false;
        String clientId = (String) auth.getPrincipal();
        // Busca la API key activa asociada al clientId
        ApiKey apiKey = apiKeyRepository.findAll().stream()
                .filter(k -> k.getClientId().equals(clientId) && k.isActive())
                .findFirst().orElse(null);
        if (apiKey == null) return false;
        // Permisos: puedes usar lógica más avanzada si lo necesitas
        return apiKey.getPermissions() != null && apiKey.getPermissions().contains(resource + ":" + action);
    }
}
