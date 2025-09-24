package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.UserDto;
import com.screenleads.backend.app.web.mapper.UserMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final CompanyRepository companyRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService perm;
    private final SecureRandom random = new SecureRandom();

    @PersistenceContext
    private EntityManager entityManager;

    public UserServiceImpl(
            UserRepository repo,
            CompanyRepository companyRepo,
            RoleRepository roleRepo,
            PasswordEncoder passwordEncoder,
            PermissionService perm) {
        this.repo = repo;
        this.companyRepo = companyRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.perm = perm;
    }

    // ---------- LECTURAS (activamos filtro en la misma Session/Tx) ----------

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAll() {
        enableCompanyFilterIfNeeded();
        return repo.findAll().stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        enableCompanyFilterIfNeeded();
        return repo.findById(id)
                .map(UserMapper::toDto)
                .orElse(null);
    }

    // ---------------------------- ESCRITURAS ----------------------------

    @Override
    @Transactional
    public void delete(Long id) {
        if (!perm.can("user", "delete")) {
            throw new IllegalArgumentException("No autorizado a borrar usuarios");
        }
        enableCompanyFilterIfNeeded(); // protege que no borre fuera de su compañía si no es admin
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public UserDto create(UserDto dto) {
        if (dto == null)
            throw new IllegalArgumentException("Body requerido");
        if (dto.getUsername() == null || dto.getUsername().isBlank())
            throw new IllegalArgumentException("username requerido");

        // Permisos para crear y jerarquía de nivel
        assertCanCreateUser();

        repo.findByUsername(dto.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("username ya existe");
        });

        User u = new User();
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setName(dto.getName());
        u.setLastName(dto.getLastName());

        // Password
        String rawPassword = (dto.getPassword() != null && !dto.getPassword().isBlank())
                ? dto.getPassword()
                : generateTempPassword(12);
        u.setPassword(passwordEncoder.encode(rawPassword));

        // Company
        if (dto.getCompanyId() != null) {
            Company c = companyRepo.findById(dto.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + dto.getCompanyId()));
            u.setCompany(c);
        } else {
            Long currentCompanyId = currentCompanyId();
            if (currentCompanyId != null && !isCurrentUserAdmin()) {
                Company c = companyRepo.findById(currentCompanyId)
                        .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + currentCompanyId));
                u.setCompany(c);
            }
        }

        // Rol ÚNICO desde el DTO
        Role role = resolveRoleFromDto(dto);
        if (role == null)
            throw new IllegalArgumentException("Se requiere un rol");

        // Verificación de jerarquía (nivel)
        assertAssignableRole(role);

        // Asignar como set (si la entidad User mantiene colección)
        u.setRole(role);

        User saved = repo.save(u);
        return UserMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto dto) {
        enableCompanyFilterIfNeeded();

        return repo.findById(id).map(existing -> {
            if (dto.getUsername() != null)
                existing.setUsername(dto.getUsername());
            if (dto.getEmail() != null)
                existing.setEmail(dto.getEmail());
            if (dto.getName() != null)
                existing.setName(dto.getName());
            if (dto.getLastName() != null)
                existing.setLastName(dto.getLastName());

            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            if (dto.getCompanyId() != null) {
                if (!isCurrentUserAdmin()) {
                    Long currentCompanyId = currentCompanyId();
                    if (currentCompanyId == null || !currentCompanyId.equals(dto.getCompanyId())) {
                        throw new IllegalArgumentException("No autorizado a cambiar de compañía");
                    }
                }
                Company c = companyRepo.findById(dto.getCompanyId())
                        .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + dto.getCompanyId()));
                existing.setCompany(c);
            }

            // Si llega un rol (único) en el DTO, cambiarlo con mismas reglas
            if (dto.getRole() != null || dto.getRole().getId() != null) {
                if (!perm.can("user", "update")) {
                    throw new IllegalArgumentException("No autorizado a actualizar usuarios");
                }
                Role newRole = resolveRoleFromDto(dto);
                if (newRole == null)
                    throw new IllegalArgumentException("Rol inválido");
                assertAssignableRole(newRole);
                existing.setRole(newRole);
            }

            User saved = repo.save(existing);
            return UserMapper.toDto(saved);
        }).orElse(null);
    }

    // ---------------------------- HELPERS ----------------------------

    private void enableCompanyFilterIfNeeded() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return;

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
        if (isAdmin)
            return;

        Long companyId = resolveCompanyId(auth);
        if (companyId == null)
            return;

        Session session = entityManager.unwrap(Session.class);
        var filter = session.getEnabledFilter("companyFilter");
        if (filter == null) {
            session.enableFilter("companyFilter").setParameter("companyId", companyId);
        } else {
            filter.setParameter("companyId", companyId);
        }
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
    }

    private Long currentCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        return resolveCompanyId(auth);
    }

    private Long resolveCompanyId(Authentication auth) {
        Object principal = auth.getPrincipal();

        if (principal instanceof User u) {
            return (u.getCompany() != null) ? u.getCompany().getId() : null;
        }
        if (principal instanceof UserDetails ud) {
            return repo.findByUsername(ud.getUsername())
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }
        if (principal instanceof String username) {
            return repo.findByUsername(username)
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }
        return null;
    }

    private String generateTempPassword(int length) {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return sb.toString();
    }

    // ======== PERMISOS / NIVELES ========

    private int currentEffectiveLevel() {
        return perm.effectiveLevel();
    }

    private void assertCanCreateUser() {
        if (!perm.can("user", "create"))
            throw new IllegalArgumentException("No autorizado a crear usuarios");
        int L = currentEffectiveLevel();
        if (L > 2) // sólo niveles 1 o 2 pueden crear
            throw new IllegalArgumentException("Solo roles de nivel 1 o 2 pueden crear usuarios");
    }

    private void assertAssignableRole(Role targetRole) {
        int myLevel = currentEffectiveLevel();
        Integer roleLevel = Optional.ofNullable(targetRole.getLevel())
                .orElse(Integer.MAX_VALUE);
        if (roleLevel < myLevel) {
            // ej.: soy nivel 2, intento asignar nivel 1 → prohibido
            throw new IllegalArgumentException("No puedes asignar un rol superior al tuyo");
        }
    }

    /**
     * Resuelve un rol único desde el DTO:
     * - Si llega roleId → busca por id.
     * - Si llega role (nombre/código) → busca por nombre.
     */
    private Role resolveRoleFromDto(UserDto dto) {
        if (dto.getRole() != null) {
            return roleRepo.findById(dto.getRole().getId())
                    .orElseThrow(() -> new IllegalArgumentException("roleId inválido: " + dto.getRole().getId()));
        }
        if (dto.getRole() != null && dto.getRole().getId() != null) {
            return roleRepo.findByRole(dto.getRole().getRole())
                    .orElseThrow(() -> new IllegalArgumentException("role inválido: " + dto.getRole()));
        }
        return null;
    }
}
