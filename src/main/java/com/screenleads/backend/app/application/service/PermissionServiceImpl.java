package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.AppEntityRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.application.service.SpringContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("perm") // 👈 clave: SpEL podrá resolver @perm
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final AppEntityRepository permissionRepository;
    private final UserRepository userRepository;

    @Override
    public boolean can(String resource, String action) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated())
                    return false;

                // Si es API_CLIENT, delega en ApiKeyPermissionService
                if (auth.getAuthorities().stream().anyMatch(a -> "API_CLIENT".equals(a.getAuthority()))) {
                    // Puedes inyectar ApiKeyPermissionService con @Autowired si lo prefieres
                    ApiKeyPermissionService apiKeyPerm = SpringContext.getBean(ApiKeyPermissionService.class);
                    return apiKeyPerm.can(resource, action);
                }

                User u = userRepository.findByUsername(auth.getName()).orElse(null);
                if (u == null || u.getRole() == null || u.getRole().getLevel() == null)
                    return false;

                int myLevel = u.getRole().getLevel();
                AppEntity p = permissionRepository.findByResource(resource).orElse(null);
                if (p == null)
                    return false;
                Integer required;
                switch (action) {
                    case "read":
                        required = p.getReadLevel();
                        break;
                    case "create":
                        required = p.getCreateLevel();
                        break;
                    case "update":
                        required = p.getUpdateLevel();
                        break;
                    case "delete":
                        required = p.getDeleteLevel();
                        break;
                    default:
                        required = null;
                        break;
                }
                if (required == null)
                    return false;
                return myLevel <= required;
            } catch (Exception e) {
                log.warn("perm.can({}, {}) falló", resource, action, e);
                return false;
            }
    }

    @Override
    public int effectiveLevel() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated())
                return Integer.MAX_VALUE;
            return userRepository.findByUsername(auth.getName())
                    .map(u -> (u.getRole() != null && u.getRole().getLevel() != null)
                            ? u.getRole().getLevel()
                            : Integer.MAX_VALUE)
                    .orElse(Integer.MAX_VALUE);
        } catch (Exception e) {
            log.warn("effectiveLevel() falló", e);
            return Integer.MAX_VALUE;
        }
    }
}
