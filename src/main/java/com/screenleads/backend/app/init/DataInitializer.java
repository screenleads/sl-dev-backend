package com.screenleads.backend.app.init;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final MediaTypeRepository mediaTypeRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt u otro

    @Override
    @Transactional
    public void run(String... args) {
        // ============ Entidades base ============
        createDefaultCompany("ScreenLeads", "Compañía por defecto para demo");

        // ===== ROLES (crea o actualiza con flags y nivel) =====
        initRoleAdmin();
        initRoleCompanyAdmin();
        initRoleCompanyManager();
        initRoleCompanyViewer();

        // ===== Tipos de media =====
        createMediaTypes("video/mp4", "mp4");
        createMediaTypes("video/webm", "webm");
        createMediaTypes("video/avi", "avi");
        createMediaTypes("video/mpeg", "mpeg");
        createMediaTypes("video/quicktime", "mov");
        createMediaTypes("video/x-msvideo", "avi");
        createMediaTypes("video/x-flv", "flv");
        createMediaTypes("image/jpeg", "jpg");
        createMediaTypes("image/png", "png");
        createMediaTypes("image/gif", "gif");
        createMediaTypes("image/webp", "webp");

        // ===== Tipos de dispositivo =====
        createDeviceTypes("tv");
        createDeviceTypes("mobile");
        createDeviceTypes("desktop");
        createDeviceTypes("tablet");
        createDeviceTypes("other");

        // ===== Usuario administrador inicial =====
        createDefaultAdminUser(
                "admin", // username
                "admin@screenleads.com", // email
                "admin123", // contraseña temporal
                "Admin", // nombre
                "Root" // apellidos
        );
    }

    // ============================================================
    // ===================== ROLES & PERMISOS =====================
    // ============================================================

    private void initRoleAdmin() {
        Role r = upsertRoleSkeleton("ROLE_ADMIN", "Acceso total", 1);

        // Admin: TODO TRUE (lectura/escritura sobre todo)
        r.setUserRead(true);
        r.setUserCreate(true);
        r.setUserUpdate(true);
        r.setUserDelete(true);
        r.setCompanyRead(true);
        r.setCompanyCreate(true);
        r.setCompanyUpdate(true);
        r.setCompanyDelete(true);
        r.setDeviceRead(true);
        r.setDeviceCreate(true);
        r.setDeviceUpdate(true);
        r.setDeviceDelete(true);
        r.setDeviceTypeRead(true);
        r.setDeviceTypeCreate(true);
        r.setDeviceTypeUpdate(true);
        r.setDeviceTypeDelete(true);
        r.setMediaRead(true);
        r.setMediaCreate(true);
        r.setMediaUpdate(true);
        r.setMediaDelete(true);
        r.setMediaTypeRead(true);
        r.setMediaTypeCreate(true);
        r.setMediaTypeUpdate(true);
        r.setMediaTypeDelete(true);
        r.setPromotionRead(true);
        r.setPromotionCreate(true);
        r.setPromotionUpdate(true);
        r.setPromotionDelete(true);
        r.setAdviceRead(true);
        r.setAdviceCreate(true);
        r.setAdviceUpdate(true);
        r.setAdviceDelete(true);
        r.setAppVersionRead(true);
        r.setAppVersionCreate(true);
        r.setAppVersionUpdate(true);
        r.setAppVersionDelete(true);

        roleRepository.save(r);
    }

    private void initRoleCompanyAdmin() {
        Role r = upsertRoleSkeleton("ROLE_COMPANY_ADMIN", "Administrador de empresa", 2);

        // Gestión de usuarios (según tu política)
        r.setUserRead(true);
        r.setUserCreate(true);
        r.setUserUpdate(true);
        r.setUserDelete(true);

        // Gestión de su empresa
        r.setCompanyRead(true);
        r.setCompanyCreate(false);
        r.setCompanyUpdate(true);
        r.setCompanyDelete(false);

        r.setDeviceRead(true);
        r.setDeviceCreate(true);
        r.setDeviceUpdate(true);
        r.setDeviceDelete(true);
        r.setMediaRead(true);
        r.setMediaCreate(true);
        r.setMediaUpdate(true);
        r.setMediaDelete(true);
        r.setPromotionRead(true);
        r.setPromotionCreate(true);
        r.setPromotionUpdate(true);
        r.setPromotionDelete(true);
        r.setAdviceRead(true);
        r.setAdviceCreate(true);
        r.setAdviceUpdate(true);
        r.setAdviceDelete(true);

        // Tipologías/Versiones: solo lectura
        r.setDeviceTypeRead(true);
        r.setDeviceTypeCreate(false);
        r.setDeviceTypeUpdate(false);
        r.setDeviceTypeDelete(false);
        r.setMediaTypeRead(true);
        r.setMediaTypeCreate(false);
        r.setMediaTypeUpdate(false);
        r.setMediaTypeDelete(false);
        r.setAppVersionRead(true);
        r.setAppVersionCreate(false);
        r.setAppVersionUpdate(false);
        r.setAppVersionDelete(false);

        roleRepository.save(r);
    }

    private void initRoleCompanyManager() {
        Role r = upsertRoleSkeleton("ROLE_COMPANY_MANAGER", "Gestor de empresa", 3);

        // No puede crear usuarios; edición limitada
        r.setUserRead(true);
        r.setUserCreate(false);
        r.setUserUpdate(true);
        r.setUserDelete(false);

        r.setCompanyRead(true);
        r.setCompanyCreate(false);
        r.setCompanyUpdate(false);
        r.setCompanyDelete(false);

        // CRUD parcial (sin delete)
        r.setDeviceRead(true);
        r.setDeviceCreate(true);
        r.setDeviceUpdate(true);
        r.setDeviceDelete(false);
        r.setMediaRead(true);
        r.setMediaCreate(true);
        r.setMediaUpdate(true);
        r.setMediaDelete(false);
        r.setPromotionRead(true);
        r.setPromotionCreate(true);
        r.setPromotionUpdate(true);
        r.setPromotionDelete(false);
        r.setAdviceRead(true);
        r.setAdviceCreate(true);
        r.setAdviceUpdate(true);
        r.setAdviceDelete(false);

        // Tipologías/Versiones: solo lectura
        r.setDeviceTypeRead(true);
        r.setDeviceTypeCreate(false);
        r.setDeviceTypeUpdate(false);
        r.setDeviceTypeDelete(false);
        r.setMediaTypeRead(true);
        r.setMediaTypeCreate(false);
        r.setMediaTypeUpdate(false);
        r.setMediaTypeDelete(false);
        r.setAppVersionRead(true);
        r.setAppVersionCreate(false);
        r.setAppVersionUpdate(false);
        r.setAppVersionDelete(false);

        roleRepository.save(r);
    }

    private void initRoleCompanyViewer() {
        Role r = upsertRoleSkeleton("ROLE_COMPANY_VIEWER", "Visualizador de empresa", 4);

        // Solo lectura
        r.setUserRead(true);
        r.setUserCreate(false);
        r.setUserUpdate(false);
        r.setUserDelete(false);
        r.setCompanyRead(true);
        r.setCompanyCreate(false);
        r.setCompanyUpdate(false);
        r.setCompanyDelete(false);
        r.setDeviceRead(true);
        r.setDeviceCreate(false);
        r.setDeviceUpdate(false);
        r.setDeviceDelete(false);
        r.setDeviceTypeRead(true);
        r.setDeviceTypeCreate(false);
        r.setDeviceTypeUpdate(false);
        r.setDeviceTypeDelete(false);
        r.setMediaRead(true);
        r.setMediaCreate(false);
        r.setMediaUpdate(false);
        r.setMediaDelete(false);
        r.setMediaTypeRead(true);
        r.setMediaTypeCreate(false);
        r.setMediaTypeUpdate(false);
        r.setMediaTypeDelete(false);
        r.setPromotionRead(true);
        r.setPromotionCreate(false);
        r.setPromotionUpdate(false);
        r.setPromotionDelete(false);
        r.setAdviceRead(true);
        r.setAdviceCreate(false);
        r.setAdviceUpdate(false);
        r.setAdviceDelete(false);
        r.setAppVersionRead(true);
        r.setAppVersionCreate(false);
        r.setAppVersionUpdate(false);
        r.setAppVersionDelete(false);

        roleRepository.save(r);
    }

    /**
     * Crea o actualiza el esqueleto del rol: si existe, actualiza descripción y
     * nivel;
     * si no, lo crea con esos valores. Devuelve la instancia a completar con flags.
     */
    private Role upsertRoleSkeleton(String roleName, String desc, int level) {
        return roleRepository.findByRole(roleName)
                .map(existing -> {
                    existing.setDescription(desc);
                    existing.setLevel(level);
                    return existing;
                })
                .orElseGet(() -> Role.builder()
                        .role(roleName)
                        .description(desc)
                        .level(level)
                        .build());
    }

    // ============================================================
    // =================== MEDIA/DEVICE/COMPANY ===================
    // ============================================================

    private void createMediaTypes(String type, String extension) {
        if (!mediaTypeRepository.existsByType(type)) {
            mediaTypeRepository.save(MediaType.builder()
                    .type(type)
                    .extension(extension)
                    .build());
        }
    }

    private void createDeviceTypes(String type) {
        if (!deviceTypeRepository.existsByType(type)) {
            deviceTypeRepository.save(DeviceType.builder()
                    .type(type)
                    .build());
        }
    }

    private void createDefaultCompany(String name, String observations) {
        if (!companyRepository.existsByName(name)) {
            companyRepository.save(Company.builder()
                    .name(name)
                    .observations(observations)
                    .build());
        }
    }

    // ============================================================
    // ======================= ADMIN USER =========================
    // ============================================================

    private void createDefaultAdminUser(String username, String email, String rawPassword, String name,
            String lastName) {
        // Si ya existe por username, no hacemos nada
        if (userRepository.existsByUsername(username)) {
            System.out.println("ℹ️  Usuario admin ya existe: " + username);
            return;
        }

        Company company = companyRepository.findByName("ScreenLeads")
                .orElseThrow(() -> new IllegalStateException("Company 'ScreenLeads' no encontrada."));
        Role adminRole = roleRepository.findByRole("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("Role 'ROLE_ADMIN' no encontrado."));

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .lastName(lastName)
                .company(company)
                .roles(Set.of(adminRole))
                // sin .enabled(true) porque tu entidad no tiene ese campo
                .build();

        userRepository.save(user);
        System.out.println(
                "✅ Usuario admin creado: " + username + " / " + email + " (cambia la contraseña tras el primer login)");
    }
}
