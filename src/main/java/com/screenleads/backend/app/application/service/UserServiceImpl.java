package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.web.dto.MediaSlimDTO;
import com.screenleads.backend.app.web.dto.MediaTypeDTO;
import com.screenleads.backend.app.web.dto.UserDto;
import com.screenleads.backend.app.web.mapper.UserMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final CompanyRepository companyRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService perm;
    private final MediaRepository mediaRepository;
    private final SecureRandom random = new SecureRandom();
    private final MediaTypeRepository mediaTypeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public UserServiceImpl(
            UserRepository repo,
            CompanyRepository companyRepo,
            RoleRepository roleRepo,
            PasswordEncoder passwordEncoder,
            PermissionService perm,
            MediaRepository mediaRepository,
            MediaTypeRepository mediaTypeRepository) {
        this.repo = repo;
        this.companyRepo = companyRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.perm = perm;
        this.mediaRepository = mediaRepository;
        this.mediaTypeRepository = mediaTypeRepository;
    }

    // ---------- LECTURAS (activamos filtro en la misma Session/Tx) ----------

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAll() {
        enableCompanyFilterIfNeeded();
        return repo.findAll().stream()
                .map(u -> {
                    if (u.getProfileImage() != null)
                        Hibernate.initialize(u.getProfileImage());
                    return u;
                })
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        enableCompanyFilterIfNeeded();
        return repo.findById(id)
                .map(u -> {
                    if (u.getProfileImage() != null)
                        Hibernate.initialize(u.getProfileImage());
                    return UserMapper.toDto(u);
                })
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
    public com.screenleads.backend.app.web.dto.UserCreationResponse create(UserDto dto) {
        validateCreateRequest(dto);
        assertCanCreateUser();

        if (repo.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("username ya existe");
        }

        User u = new User();
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setName(dto.getName());
        u.setLastName(dto.getLastName());

        String rawPassword = setPasswordForNewUser(dto, u);
        assignCompanyToNewUser(dto, u);
        assignProfileImageToNewUser(dto, u);
        assignRoleToNewUser(dto, u);

        User saved = repo.save(u);
        return new com.screenleads.backend.app.web.dto.UserCreationResponse(
                UserMapper.toDto(saved),
                (dto.getPassword() != null && !dto.getPassword().isBlank()) ? null : rawPassword);
    }

    private void validateCreateRequest(UserDto dto) {
        if (dto == null)
            throw new IllegalArgumentException("Body requerido");
        if (dto.getUsername() == null || dto.getUsername().isBlank())
            throw new IllegalArgumentException("username requerido");
    }

    private String setPasswordForNewUser(UserDto dto, User u) {
        String rawPassword = (dto.getPassword() != null && !dto.getPassword().isBlank())
                ? dto.getPassword()
                : generateTempPassword(12);
        u.setPassword(passwordEncoder.encode(rawPassword));
        return rawPassword;
    }

    private void assignCompanyToNewUser(UserDto dto, User u) {
        Long companyId = extractCompanyId(dto);
        if (companyId == null)
            return;

        validateCompanyAssignment(companyId);
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + companyId));
        u.setCompany(company);
    }

    private void assignProfileImageToNewUser(UserDto dto, User u) {
        if (dto.getProfileImage() == null || dto.getProfileImage().src() == null)
            return;

        var mediaOpt = mediaRepository.findBySrc(dto.getProfileImage().src());
        if (mediaOpt.isPresent()) {
            u.setProfileImage(mediaOpt.get());
        } else {
            Media newMedia = createMediaFromDto(dto.getProfileImage(), u.getCompany(), dto);
            u.setProfileImage(mediaRepository.save(newMedia));
        }
    }

    private void assignRoleToNewUser(UserDto dto, User u) {
        Role role = resolveRoleFromDto(dto);
        if (role == null)
            throw new IllegalArgumentException("Se requiere un rol");
        assertAssignableRole(role);
        u.setRole(role);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto dto) {
        enableCompanyFilterIfNeeded();

        return repo.findById(id).map(existing -> {
            updateBasicFields(dto, existing);
            updateCompanyIfPresent(dto, existing);
            updateProfileImageIfPresent(dto, existing);
            updateRoleIfPresent(dto, existing);

            User saved = repo.save(existing);
            return UserMapper.toDto(saved);
        }).orElse(null);
    }

    private void updateBasicFields(UserDto dto, User existing) {
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
    }

    private void updateCompanyIfPresent(UserDto dto, User existing) {
        Long finalCompanyId = extractCompanyId(dto);
        if (finalCompanyId == null)
            return;

        validateCompanyAssignment(finalCompanyId);
        Company c = companyRepo.findById(finalCompanyId)
                .orElseThrow(() -> new IllegalArgumentException("companyId inválido: " + finalCompanyId));
        existing.setCompany(c);
    }

    private void updateProfileImageIfPresent(UserDto dto, User existing) {
        if (dto.getProfileImage() == null || dto.getProfileImage().src() == null)
            return;

        var mediaOpt = mediaRepository.findBySrc(dto.getProfileImage().src());
        if (mediaOpt.isPresent()) {
            existing.setProfileImage(mediaOpt.get());
        } else {
            Media newMedia = createMediaFromDto(dto.getProfileImage(), existing.getCompany(), dto);
            existing.setProfileImage(mediaRepository.save(newMedia));
        }
    }

    private void updateRoleIfPresent(UserDto dto, User existing) {
        if (dto.getRole() == null)
            return;

        if (!perm.can("user", "update"))
            throw new IllegalArgumentException("No autorizado a actualizar usuarios");
        Role newRole = resolveRoleFromDto(dto);
        if (newRole == null)
            throw new IllegalArgumentException("Rol inválido");
        assertAssignableRole(newRole);
        existing.setRole(newRole);
    }

    // ---------------------------- HELPERS ----------------------------

    private Long extractCompanyId(UserDto dto) {
        if (dto.getCompanyId() != null) {
            return dto.getCompanyId();
        } else if (dto.getCompany() != null && dto.getCompany().id() != null) {
            return dto.getCompany().id();
        }
        return null;
    }

    private void validateCompanyAssignment(Long companyId) {
        if (isCurrentUserAdmin())
            return;

        Long currentCompanyId = currentCompanyId();
        if (currentCompanyId == null || !currentCompanyId.equals(companyId)) {
            throw new IllegalArgumentException("No autorizado a cambiar de compañía");
        }
    }

    private Media createMediaFromDto(MediaSlimDTO mediaDto, Company userCompany, UserDto dto) {
        MediaType type = resolveMediaType(mediaDto);
        Company company = resolveCompanyForMedia(userCompany, dto);

        return Media.builder()
                .src(mediaDto.src())
                .type(type)
                .company(company)
                .build();
    }

    private MediaType resolveMediaType(MediaSlimDTO mediaDto) {
        MediaTypeDTO typeDto = mediaDto.type();

        // Si viene con typeDto y tiene id, buscarlo
        if (typeDto != null && typeDto.id() != null) {
            return mediaTypeRepository.findById(typeDto.id()).orElse(null);
        }

        // Si viene con type name, buscar por nombre
        if (typeDto != null && typeDto.type() != null && !typeDto.type().isBlank()) {
            return mediaTypeRepository.findByType(typeDto.type()).orElse(null);
        }

        // Fallback: extraer extensión del src
        String src = mediaDto.src();
        String extension = extractExtension(src);
        MediaType type = findMediaTypeByExtension(extension).orElse(null);

        if (type == null) {
            logMediaTypeError(extension, src);
            throw new IllegalArgumentException(
                    "No se pudo determinar el tipo de media para la imagen de perfil (extensión: "
                            + extension + "). Asegúrate de que el MediaType existe.");
        }
        return type;
    }

    private Optional<MediaType> findMediaTypeByExtension(String extension) {
        if (extension == null)
            return Optional.empty();
        return mediaTypeRepository.findByExtensionIgnoreCase(extension);
    }

    private String extractExtension(String src) {
        if (src == null || !src.contains("."))
            return null;

        String extCandidate = src.substring(src.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        int qIdx = extCandidate.indexOf('?');
        return qIdx > 0 ? extCandidate.substring(0, qIdx) : extCandidate;
    }

    private void logMediaTypeError(String extension, String src) {
        var allMediaTypes = mediaTypeRepository.findAll();
        log.error("[MEDIA PROFILE] No se encontró MediaType con extensión: {}. Payload src: {}", extension, src);
        log.error("[MEDIA PROFILE] MediaTypes en BD:");
        for (MediaType mt : allMediaTypes) {
            log.error("  - id: {}, type: {}, extension: '{}', enabled: {}",
                    mt.getId(), mt.getType(), mt.getExtension(), mt.getEnabled());
        }
        log.error("[MEDIA PROFILE] Valor de extensión buscada (TRIM, lower): '{}', original: '{}'",
                extension != null ? extension.trim().toLowerCase(Locale.ROOT) : null, extension);
    }

    private Company resolveCompanyForMedia(Company userCompany, UserDto dto) {
        if (userCompany != null)
            return userCompany;

        Long companyId = extractCompanyId(dto);
        if (companyId != null) {
            return companyRepo.findById(companyId)
                    .orElseThrow(
                            () -> new IllegalArgumentException("No se puede asociar media: compañía no encontrada"));
        }

        throw new IllegalArgumentException("No se puede asociar media: compañía no encontrada");
    }

    private void enableCompanyFilterIfNeeded() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return;

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(Set.of("ROLE_ADMIN", "ADMIN")::contains);
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
                .anyMatch(Set.of("ROLE_ADMIN", "ADMIN")::contains);
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
            return Optional.ofNullable(u.getCompany())
                    .map(Company::getId)
                    .orElse(null);
        }

        String username = extractUsername(principal);
        if (username == null)
            return null;

        return repo.findByUsername(username)
                .map(User::getCompany)
                .map(Company::getId)
                .orElse(null);
    }

    private String extractUsername(Object principal) {
        if (principal instanceof UserDetails ud)
            return ud.getUsername();
        if (principal instanceof String username)
            return username;
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
        int currentLevel = currentEffectiveLevel();
        if (currentLevel > 2) // sólo niveles 1 o 2 pueden crear
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
        if (dto.getRole() == null)
            return null;
        if (dto.getRole().id() != null) {
            return roleRepo.findById(dto.getRole().id())
                    .orElseThrow(() -> new IllegalArgumentException("roleId inválido: " + dto.getRole().id()));
        }
        if (dto.getRole().role() != null) {
            return roleRepo.findByRole(dto.getRole().role())
                    .orElseThrow(() -> new IllegalArgumentException("role inválido: " + dto.getRole().role()));
        }
        return null;
    }

}
