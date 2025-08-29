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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final CompanyRepository companyRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @PersistenceContext
    private EntityManager entityManager;

    public UserServiceImpl(
            UserRepository repo,
            CompanyRepository companyRepo,
            RoleRepository roleRepo,
            PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.companyRepo = companyRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
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
        enableCompanyFilterIfNeeded(); // protege que no borre fuera de su compañía
        repo.deleteById(id);
    }

    @Override
    @Transactional
    public UserDto create(UserDto dto) {
        if (dto == null)
            throw new IllegalArgumentException("Body requerido");
        if (dto.getUsername() == null || dto.getUsername().isBlank())
            throw new IllegalArgumentException("username requerido");

        repo.findByUsername(dto.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("username ya existe");
        });

        User u = new User();
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setName(dto.getName());
        u.setLastName(dto.getLastName());

        // Generación/establecimiento de password
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
            // Si no viene companyId y el usuario no es admin, forzar su compañía
            Long currentCompanyId = currentCompanyId();
            if (currentCompanyId != null && !isCurrentUserAdmin()) {
                Company c = companyRepo.findById(currentCompanyId)
                        .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + currentCompanyId));
                u.setCompany(c);
            }
        }

        // Roles
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String rn : dto.getRoles()) {
                Role r = roleRepo.findByRole(rn)
                        .orElseThrow(() -> new IllegalArgumentException("role inválido: " + rn));
                roles.add(r);
            }
            u.setRoles(roles);
        } else {
            u.setRoles(Set.of());
        }

        User saved = repo.save(u);
        return UserMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto dto) {
        enableCompanyFilterIfNeeded(); // asegura que solo se actualicen usuarios de su compañía (no admin)

        return repo.findById(id).map(existing -> {
            if (dto.getUsername() != null)
                existing.setUsername(dto.getUsername());
            if (dto.getEmail() != null)
                existing.setEmail(dto.getEmail());
            if (dto.getName() != null)
                existing.setName(dto.getName());
            if (dto.getLastName() != null)
                existing.setLastName(dto.getLastName());

            // Permitir cambio de password
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            // Cambiar compañía (si se permite)
            if (dto.getCompanyId() != null) {
                // Si el actual no es admin, no permitir cambiar a otra compañía distinta de la
                // suya
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

            // Roles
            if (dto.getRoles() != null) {
                Set<Role> roles = new HashSet<>();
                for (String rn : dto.getRoles()) {
                    Role r = roleRepo.findByRole(rn)
                            .orElseThrow(() -> new IllegalArgumentException("role inválido: " + rn));
                    roles.add(r);
                }
                existing.setRoles(roles);
            }

            User saved = repo.save(existing);
            return UserMapper.toDto(saved);
        }).orElse(null);
    }

    // ---------------------------- HELPERS ----------------------------

    /**
     * Activa el filtro "companyFilter" en la misma Session/Tx usada por el
     * repositorio
     * cuando el usuario actual NO es admin.
     */
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

        // 1) Entidad de dominio como principal
        if (principal instanceof User u) {
            return (u.getCompany() != null) ? u.getCompany().getId() : null;
        }

        // 2) UserDetails estándar
        if (principal instanceof UserDetails ud) {
            return repo.findByUsername(ud.getUsername())
                    .map(u -> u.getCompany() != null ? u.getCompany().getId() : null)
                    .orElse(null);
        }

        // 3) Principal como String (username), p.ej. JWT con "sub"
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
}
