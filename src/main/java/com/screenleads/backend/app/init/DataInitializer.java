package com.screenleads.backend.app.init;

import java.util.Optional;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.model.EntityPermission;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.domain.repositories.EntityPermissionRepository;
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
    private final PasswordEncoder passwordEncoder;

    // NUEVO
    private final EntityPermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // ============ Entidades base ============
        createDefaultCompany("ScreenLeads", "Compañía por defecto para demo");

        // ===== ROLES (crea o actualiza con nivel) =====
        Role admin = upsertRole("ROLE_ADMIN", "Acceso total", 1);
        Role companyAdm = upsertRole("ROLE_COMPANY_ADMIN", "Administrador de empresa", 2);
        Role companyMgr = upsertRole("ROLE_COMPANY_MANAGER", "Gestor de empresa", 3);
        Role companyVwr = upsertRole("ROLE_COMPANY_VIEWER", "Visualizador de empresa", 4);

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
                "admin",
                "admin@screenleads.com",
                "admin123",
                "Admin",
                "Root",
                admin);

        // ===== NUEVO: Permisos por entidad (niveles) =====
        seedEntityPermissionsByEntity();
    }

    // ============================================================
    // ===================== ROLES (nivel) ========================
    // ============================================================

    private Role upsertRole(String roleName, String desc, int level) {
        Optional<Role> opt = roleRepository.findByRole(roleName);
        if (opt.isPresent()) {
            Role r = opt.get();
            r.setDescription(desc);
            r.setLevel(level);
            return roleRepository.save(r);
        }
        Role r = Role.builder()
                .role(roleName)
                .description(desc)
                .level(level)
                .build();
        return roleRepository.save(r);
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

    private void createDefaultAdminUser(String username,
            String email,
            String rawPassword,
            String name,
            String lastName,
            Role adminRole) {
        if (userRepository.existsByUsername(username)) {
            System.out.println("ℹ️  Usuario admin ya existe: " + username);
            return;
        }

        Company company = companyRepository.findByName("ScreenLeads")
                .orElseThrow(() -> new IllegalStateException("Company 'ScreenLeads' no encontrada."));

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .lastName(lastName)
                .company(company)
                // Si tu entidad User tiene UN solo rol:
                .role(adminRole)
                // Si aún usa Set<Role>, usa:
                // .roles(Set.of(adminRole))
                .build();

        userRepository.save(user);
        System.out.println(
                "✅ Usuario admin creado: " + username + " / " + email + " (cambia la contraseña tras el primer login)");
    }

    // ============================================================
    // ============== NUEVO: PERMISOS POR ENTIDAD =================
    // ============================================================

    private void seedEntityPermissionsByEntity() {

        upsertEntityPermission("company", 1, 1, 1, 1);
        upsertEntityPermission("deviceType", 1, 1, 1, 1);
        upsertEntityPermission("mediaType", 1, 1, 1, 1);
        upsertEntityPermission("role", 1, 1, 1, 1);
        upsertEntityPermission("EntityPermission", 1, 1, 1, 1);
        upsertEntityPermission("appVersion", 1, 1, 1, 1);

        upsertEntityPermission("promotionLead", 2, 2, 2, 2);
        upsertEntityPermission("customer", 2, 2, 2, 2);

        upsertEntityPermission("user", 3, 2, 2, 2);

        upsertEntityPermission("advice", 4, 3, 3, 3);
        upsertEntityPermission("promotion", 4, 3, 3, 3);
        upsertEntityPermission("coupon", 4, 3, 3, 3);
        upsertEntityPermission("media", 4, 3, 3, 3);

        upsertEntityPermission("device", 4, 4, 3, 3);

    }

    private void upsertEntityPermission(String resource, Integer read, Integer create, Integer update, Integer delete) {
        Optional<EntityPermission> opt = permissionRepository.findByResource(resource);
        if (opt.isEmpty()) {
            permissionRepository.save(EntityPermission.builder()
                    .resource(resource)
                    .readLevel(read)
                    .createLevel(create)
                    .updateLevel(update)
                    .deleteLevel(delete)
                    .build());
            return;
        }
        EntityPermission p = opt.get();
        boolean changed = false;
        if (!read.equals(p.getReadLevel())) {
            p.setReadLevel(read);
            changed = true;
        }
        if (!create.equals(p.getCreateLevel())) {
            p.setCreateLevel(create);
            changed = true;
        }
        if (!update.equals(p.getUpdateLevel())) {
            p.setUpdateLevel(update);
            changed = true;
        }
        if (!delete.equals(p.getDeleteLevel())) {
            p.setDeleteLevel(delete);
            changed = true;
        }
        if (changed)
            permissionRepository.save(p);
    }
}
