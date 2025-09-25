# Inicializadores & Seeds — snapshot incrustado

> DataInitializer, loaders, etc.

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/init/DataInitializer.java
package com.screenleads.backend.app.init;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.domain.repositories.AppEntityRepository;

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

    private final AppEntityRepository appEntityRepository;

    @Override
    @Transactional
    public void run(String... args) {
        createDefaultCompany("ScreenLeads", "Compañía por defecto para demo");

        Role admin = upsertRole("ROLE_ADMIN", "Acceso total", 1);
        upsertRole("ROLE_COMPANY_ADMIN", "Administrador de empresa", 2);
        upsertRole("ROLE_COMPANY_MANAGER", "Gestor de empresa", 3);
        upsertRole("ROLE_COMPANY_VIEWER", "Visualizador de empresa", 4);

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

        createDeviceTypes("tv");
        createDeviceTypes("mobile");
        createDeviceTypes("desktop");
        createDeviceTypes("tablet");
        createDeviceTypes("other");

        createDefaultAdminUser(
                "admin",
                "admin@screenleads.com",
                "admin123",
                "Admin",
                "Root",
                admin);

        seedAppEntitiesWithPermissionsAndDashboard();
    }

    private Role upsertRole(String roleName, String desc, int level) {
        Optional<Role> opt = roleRepository.findByRole(roleName);
        if (opt.isPresent()) {
            Role r = opt.get();
            r.setDescription(desc);
            r.setLevel(level);
            return roleRepository.save(r);
        }
        Role r = Role.builder().role(roleName).description(desc).level(level).build();
        return roleRepository.save(r);
    }

    private void createMediaTypes(String type, String extension) {
        if (!mediaTypeRepository.existsByType(type)) {
            mediaTypeRepository.save(MediaType.builder().type(type).extension(extension).build());
        }
    }

    private void createDeviceTypes(String type) {
        if (!deviceTypeRepository.existsByType(type)) {
            deviceTypeRepository.save(DeviceType.builder().type(type).build());
        }
    }

    private void createDefaultCompany(String name, String observations) {
        if (!companyRepository.existsByName(name)) {
            companyRepository.save(Company.builder().name(name).observations(observations).build());
        }
    }

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
                .role(adminRole)
                .build();

        userRepository.save(user);
        System.out.println("✅ Usuario admin creado: " + username + " / " + email);
    }

    // ---------- seed app_entity con permisos + metadatos de dashboard ----------

    private void seedAppEntitiesWithPermissionsAndDashboard() {
        int order = 0;
        Map<String, String> attrs;

        // Company
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("name","String"); attrs.put("observations","String");
        attrs.put("primaryColor","String"); attrs.put("secondaryColor","String");
        upsertAppEntity("company", "Company",
                "com.screenleads.backend.app.domain.model.Company", "company", "Long",
                "/companies",
                1, 1, 1, 1, null, attrs,
                "Companies", "building-2", order += 10);

        // Device
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("uuid","String"); attrs.put("name","String");
        upsertAppEntity("device", "Device",
                "com.screenleads.backend.app.domain.model.Device", "device", "Long",
                "/devices",
                4, 4, 3, 3, null, attrs,
                "Devices", "tv-2", order += 10);

        // DeviceType
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("type","String"); attrs.put("enabled","Boolean");
        upsertAppEntity("device_type", "DeviceType",
                "com.screenleads.backend.app.domain.model.DeviceType", "device_type", "Long",
                "/device-types",
                1, 1, 1, 1, null, attrs,
                "Device Types", "boxes", order += 10);

        // Media
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("name","String"); attrs.put("src","String");
        upsertAppEntity("media", "Media",
                "com.screenleads.backend.app.domain.model.Media", "media", "Long",
                "/media",
                3, 4, 3, 3, null, attrs,
                "Media", "image", order += 10);

        // MediaType
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("enabled","Boolean"); attrs.put("type","String"); attrs.put("extension","String");
        upsertAppEntity("media_type", "MediaType",
                "com.screenleads.backend.app.domain.model.MediaType", "media_type", "Long",
                "/media-types",
                1, 1, 1, 1, null, attrs,
                "Media Types", "file-cog", order += 10);

        // Advice
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("description","String"); attrs.put("customInterval","Boolean"); attrs.put("interval","Duration");
        upsertAppEntity("advice", "Advice",
                "com.screenleads.backend.app.domain.model.Advice", "advice", "Long",
                "/advices",
                3, 4, 3, 3, null, attrs,
                "Advices", "calendar-clock", order += 10);

        // AdviceSchedule
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("startDate","LocalDate"); attrs.put("endDate","LocalDate");
        upsertAppEntity("advice_schedule", "AdviceSchedule",
                "com.screenleads.backend.app.domain.model.AdviceSchedule", "advice_schedule", "Long",
                "/advice-schedules",
                3, 4, 3, 3, null, attrs,
                "Advice Schedules", "calendar-range", order += 10);

        // AdviceTimeWindow
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("weekday","DayOfWeek"); attrs.put("fromTime","LocalTime"); attrs.put("toTime","LocalTime");
        upsertAppEntity("advice_time_window", "AdviceTimeWindow",
                "com.screenleads.backend.app.domain.model.AdviceTimeWindow", "advice_time_window", "Long",
                "/advice-time-windows",
                3, 4, 3, 3, null, attrs,
                "Advice Time Windows", "clock-4", order += 10);

        // Promotion
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("name","String"); attrs.put("description","String"); attrs.put("legalUrl","String");
        upsertAppEntity("promotion", "Promotion",
                "com.screenleads.backend.app.domain.model.Promotion", "promotion", "Long",
                "/promotions",
                3, 4, 3, 3, null, attrs,
                "Promotions", "ticket-percent", order += 10);

        // PromotionLead
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("identifierType","LeadIdentifierType"); attrs.put("identifier","String");
        attrs.put("couponCode","String"); attrs.put("couponStatus","CouponStatus");
        upsertAppEntity("promotion_lead", "PromotionLead",
                "com.screenleads.backend.app.domain.model.PromotionLead", "promotion_lead", "Long",
                "/promotion-leads",
                2, 2, 2, 2, null, attrs,
                "Promotion Leads", "users-round", order += 10);

        // Customer
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("firstName","String"); attrs.put("lastName","String");
        attrs.put("identifierType","LeadIdentifierType"); attrs.put("identifier","String");
        upsertAppEntity("customer", "Customer",
                "com.screenleads.backend.app.domain.model.Customer", "customer", "Long",
                "/customers",
                2, 2, 2, 2, null, attrs,
                "Customers", "contact", order += 10);

        // User
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("username","String"); attrs.put("email","String"); attrs.put("name","String"); attrs.put("lastName","String");
        upsertAppEntity("user", "User",
                "com.screenleads.backend.app.domain.model.User", "app_user", "Long",
                "/users",
                2, 3, 2, 2, null, attrs,
                "Users", "user", order += 10);

        // Role
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("role","String"); attrs.put("description","String"); attrs.put("level","Integer");
        upsertAppEntity("role", "Role",
                "com.screenleads.backend.app.domain.model.Role", "role", "Long",
                "/roles",
                1, 1, 1, 1, null, attrs,
                "Roles", "shield", order += 10);

        // AppVersion
        attrs = new LinkedHashMap<>();
        attrs.put("id","Long"); attrs.put("platform","String"); attrs.put("version","String");
        attrs.put("message","String"); attrs.put("url","String"); attrs.put("forceUpdate","boolean");
        upsertAppEntity("app_version", "AppVersion",
                "com.screenleads.backend.app.domain.model.AppVersion", "app_version", "Long",
                "/app-versions",
                1, 1, 1, 1, null, attrs,
                "App Versions", "download-cloud", order += 10);
    }

    private void upsertAppEntity(
            String resource, String entityName,
            String className, String tableName, String idType,
            String endpointBase,
            Integer createLevel, Integer readLevel,
            Integer updateLevel, Integer deleteLevel,
            Long rowCount,
            Map<String, String> attributes,
            String displayLabel, String icon, Integer sortOrder) {

        Optional<AppEntity> opt = appEntityRepository.findByResource(resource);
        AppEntity e = opt.orElseGet(() -> AppEntity.builder().resource(resource).build());

        boolean changed = false;

        if (!entityName.equals(e.getEntityName())) { e.setEntityName(entityName); changed = true; }
        if (className != null && !className.equals(e.getClassName())) { e.setClassName(className); changed = true; }
        if (tableName != null && !tableName.equals(e.getTableName())) { e.setTableName(tableName); changed = true; }
        if (idType != null && !idType.equals(e.getIdType())) { e.setIdType(idType); changed = true; }
        if (!endpointBase.equals(e.getEndpointBase())) { e.setEndpointBase(endpointBase); changed = true; }

        if (e.getCreateLevel() == null || !createLevel.equals(e.getCreateLevel())) { e.setCreateLevel(createLevel); changed = true; }
        if (e.getReadLevel()   == null || !readLevel.equals(e.getReadLevel()))     { e.setReadLevel(readLevel);     changed = true; }
        if (e.getUpdateLevel() == null || !updateLevel.equals(e.getUpdateLevel())) { e.setUpdateLevel(updateLevel); changed = true; }
        if (e.getDeleteLevel() == null || !deleteLevel.equals(e.getDeleteLevel())) { e.setDeleteLevel(deleteLevel); changed = true; }

        if (rowCount != null && (e.getRowCount() == null || !rowCount.equals(e.getRowCount()))) {
            e.setRowCount(rowCount); changed = true;
        }

        if (attributes != null && !attributes.equals(e.getAttributes())) {
            e.setAttributes(new LinkedHashMap<>(attributes)); changed = true;
        }

        // Dashboard metadata
        if (displayLabel != null && !displayLabel.equals(e.getDisplayLabel())) { e.setDisplayLabel(displayLabel); changed = true; }
        if ((icon != null && !icon.equals(e.getIcon())) || (icon == null && e.getIcon() != null)) { e.setIcon(icon); changed = true; }
        if ((sortOrder != null && !sortOrder.equals(e.getSortOrder())) || (sortOrder == null && e.getSortOrder() != null)) { e.setSortOrder(sortOrder); changed = true; }

        if (opt.isEmpty() || changed) {
            appEntityRepository.save(e);
        }
    }
}

```

