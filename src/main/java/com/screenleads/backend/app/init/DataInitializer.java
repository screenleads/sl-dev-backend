package com.screenleads.backend.app.init;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.AppEntityAttribute;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.AppEntityRepository;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;

import lombok.RequiredArgsConstructor;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.application.service.AdviceService;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.CompanyRefDTO;
import com.screenleads.backend.app.web.dto.MediaUpsertDTO;
import com.screenleads.backend.app.web.dto.AdviceScheduleDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

        record AppEntityConfig(
                String resource, String entityName, String className, String tableName, String idType,
                String endpointBase, Integer createLevel, Integer readLevel, Integer updateLevel, 
                Integer deleteLevel, Boolean visibleInMenu, Long rowCount, String displayLabel, 
                String icon, Integer sortOrder) {
        }

        private final RoleRepository roleRepository;
        private final MediaTypeRepository mediaTypeRepository;
        private final DeviceTypeRepository deviceTypeRepository;
        private final CompanyRepository companyRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        private final AppEntityRepository appEntityRepository;
        private final MediaRepository mediaRepository;
        private final AdviceRepository adviceRepository;
        private final AdviceService adviceService;

        @PersistenceContext
        private EntityManager em;

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

                createDefaultAdminUser("admin", "admin@screenleads.com", "admin123", "Admin", "Root", admin);

                // 1) Esqueleto de AppEntity (sin atributos), con sortOrder único
                seedAppEntitiesSkeleton();

                // 2) Bootstrap de atributos desde el metamodelo JPA (nuevas entidades al final)
                bootstrapEntityAttributesFromMetamodel();

                // === CREAR ANUNCIO MOCK SI NO EXISTEN Y ESTAMOS EN DESARROLLO ===
                String activeProfile = System.getProperty("spring.profiles.active", "dev");
                if (Set.of("dev", "development").contains(activeProfile.toLowerCase())) {
                        try {
                                Company company = companyRepository.findByName("ScreenLeads").orElse(null);
                                if (company != null && adviceRepository.count() == 0) {
                                        log.info("[MOCK] Creando anuncio de test por inicialización...");
                                        String mediaSrc = "https://storage.googleapis.com/screenleads-e7e0b.firebasestorage.app/media/videos/compressed-e69233c4-260a-4b3c-8ba6-876c34989725-tv_desayunos_1.mp4";
                                        Long mediaId = mediaRepository.findBySrc(mediaSrc).map(m -> m.getId())
                                                        .orElse(null);
                                        AdviceDTO mockDto = new AdviceDTO(
                                                        null,
                                                        "Anuncio de test ",
                                                        false,
                                                        0,
                                                        new MediaUpsertDTO(mediaId, mediaSrc),
                                                        null,
                                                        new CompanyRefDTO(company.getId(), company.getName()),
                                                        Arrays.asList(
                                                                        new AdviceScheduleDTO(
                                                                                        null,
                                                                                        "2025-09-30T22:00:00.000Z",
                                                                                        "2025-10-30T23:00:00.000Z",
                                                                                        Arrays.asList(
                                                                                                        new com.screenleads.backend.app.web.dto.AdviceTimeWindowDTO(
                                                                                                                        null,
                                                                                                                        "MONDAY",
                                                                                                                        "00:00",
                                                                                                                        "23:59"),
                                                                                                        new com.screenleads.backend.app.web.dto.AdviceTimeWindowDTO(
                                                                                                                        null,
                                                                                                                        "SUNDAY",
                                                                                                                        "00:00",
                                                                                                                        "23:59")),
                                                                                        null)));
                                        adviceService.saveAdvice(mockDto);
                                }
                        } catch (Exception e) {
                                log.warn("[MOCK] Error creando anuncio de test: {}", e.getMessage());
                        }
                }
        }

        // ========= helpers sortOrder =========
        /** Máximo sortOrder actual (ignora nulls). */
        private int computeMaxSortOrder() {
                return appEntityRepository.findAll().stream()
                                .map(AppEntity::getSortOrder)
                                .filter(Objects::nonNull)
                                .max(Integer::compareTo)
                                .orElse(0);
        }

        /**
         * Asegura un sortOrder único para menú. Si desired es null -> siguiente libre.
         * Excluye la propia entidad por resource (si existe).
         */
        private int ensureUniqueSortOrder(Integer desired, String selfResource) {
                List<AppEntity> all = appEntityRepository.findAll();
                Set<Integer> used = new HashSet<>();
                int max = 0;
                for (AppEntity e : all) {
                        if (Objects.equals(e.getResource(), selfResource))
                                continue;
                        if (Boolean.TRUE.equals(e.getVisibleInMenu()) && e.getSortOrder() != null) {
                                used.add(e.getSortOrder());
                        }
                }
                int n = (desired != null) ? desired : (max + 1);
                while (used.contains(n))
                        n++;
                return n;
        }

        // ========= SEED ENTIDADES (sólo esqueleto) =========
        private void seedAppEntitiesSkeleton() {
                                // Eliminado: lógica de CompanyToken
                // Para evitar choques, pasamos sortOrder deseado (o null) y dentro se normaliza
                upsertAppEntity(new AppEntityConfig("company", "Company",
                                "com.screenleads.backend.app.domain.model.Company", "company", "Long",
                                "/companies", 1, 1, 1, 1, true, null,
                                "Companies", "building-2", 1));

                upsertAppEntity(new AppEntityConfig("device", "Device",
                                "com.screenleads.backend.app.domain.model.Device", "device", "Long",
                                "/devices", 4, 4, 3, 3, true, null,
                                "Devices", "tv-2", 2));

                upsertAppEntity(new AppEntityConfig("device_type", "DeviceType",
                                "com.screenleads.backend.app.domain.model.DeviceType", "device_type", "Long",
                                "/devices/types", 1, 1, 1, 1, true, null,
                                "Device Types", "devices_other", 3));

                upsertAppEntity(new AppEntityConfig("media", "Media",
                                "com.screenleads.backend.app.domain.model.Media", "media", "Long",
                                "/medias", 3, 3, 3, 3, true, null,
                                "Medias", "perm_media", 4));

                upsertAppEntity(new AppEntityConfig("media_type", "MediaType",
                                "com.screenleads.backend.app.domain.model.MediaType", "media_type", "Long",
                                "/medias/types", 1, 1, 1, 1, true, null,
                                "Media Types", "perm_media", 5));

                upsertAppEntity(new AppEntityConfig("advice", "Advice",
                                "com.screenleads.backend.app.domain.model.Advice", "advice", "Long",
                                "/advices", 3, 4, 3, 3, true, null,
                                "Advices", "image_inset", 6));

                upsertAppEntity(new AppEntityConfig("promotion", "Promotion",
                                "com.screenleads.backend.app.domain.model.Promotion", "promotion", "Long",
                                "/promotions", 3, 4, 3, 3, true, null,
                                "Promotions", "campaign", 7));

                upsertAppEntity(new AppEntityConfig("customer", "Customer",
                                "com.screenleads.backend.app.domain.model.Customer", "customer", "Long",
                                "/customers", 2, 2, 2, 2, true, null,
                                "Customers", "man_4", 8));

                upsertAppEntity(new AppEntityConfig("user", "User",
                                "com.screenleads.backend.app.domain.model.User", "app_user", "Long",
                                "/users", 2, 3, 2, 2, true, null,
                                "Users", "account_circle", 9));

                upsertAppEntity(new AppEntityConfig("role", "Role",
                                "com.screenleads.backend.app.domain.model.Role", "role", "Long",
                                "/roles", 1, 1, 1, 1, true, null,
                                "Roles", "shield", 10));

                upsertAppEntity(new AppEntityConfig("app_version", "AppVersion",
                                "com.screenleads.backend.app.domain.model.AppVersion", "app_version", "Long",
                                "/app-versions", 1, 1, 1, 1, true, null,
                                "App Versions", "download-cloud", 11));

                upsertAppEntity(new AppEntityConfig("api_key", "ApiKey",
                                "com.screenleads.backend.app.domain.model.ApiKey", "api_key", "Long",
                                "/api-keys", 1, 3, 2, 2, true, null,
                                "API Keys", "vpn_key", 13));

                // AppEntity (metamodelo) visible en el menú -> sortOrder único (pasa null o
                // repetido, se corrige)
                upsertAppEntity(new AppEntityConfig("app_entity", "AppEntity",
                                "com.screenleads.backend.app.domain.model.AppEntity", "app_entity", "Long",
                                "/entities", 1, 4, 3, 3, true, null,
                                "App Entities", "apps", null)); // null => siguiente libre

                // AppEntityAttribute (metamodelo) visible en el menú -> sortOrder único
                upsertAppEntity(new AppEntityConfig("app_entity_attribute", "AppEntityAttribute",
                                "com.screenleads.backend.app.domain.model.AppEntityAttribute", "app_entity_attribute",
                                "Long",
                                "/app-entity-attributes", 1, 4, 3, 3, true, null,
                                "App Entity Attributes", "list", null)); // null => siguiente libre
        }

        private void upsertAppEntity(AppEntityConfig config) {
                Optional<AppEntity> opt = appEntityRepository.findByResource(config.resource());
                AppEntity e = opt.orElseGet(() -> AppEntity.builder().resource(config.resource()).build());

                boolean changed = updateEntityFields(e, config);
                Integer safeSortOrder = calculateSortOrder(config);
                
                if (!Objects.equals(safeSortOrder, e.getSortOrder())) {
                        e.setSortOrder(safeSortOrder);
                        changed = true;
                }

                if (opt.isEmpty() || changed) {
                        appEntityRepository.save(e);
                }
        }

        private boolean updateEntityFields(AppEntity e, AppEntityConfig config) {
                boolean changed = false;

                changed |= updateField(e.getEntityName(), config.entityName(), e::setEntityName);
                changed |= updateField(e.getClassName(), config.className(), e::setClassName);
                changed |= updateField(e.getTableName(), config.tableName(), e::setTableName);
                changed |= updateField(e.getIdType(), config.idType(), e::setIdType);
                changed |= updateField(e.getEndpointBase(), config.endpointBase(), e::setEndpointBase);
                changed |= updateField(e.getCreateLevel(), config.createLevel(), e::setCreateLevel);
                changed |= updateField(e.getReadLevel(), config.readLevel(), e::setReadLevel);
                changed |= updateField(e.getUpdateLevel(), config.updateLevel(), e::setUpdateLevel);
                changed |= updateField(e.getDeleteLevel(), config.deleteLevel(), e::setDeleteLevel);
                changed |= updateField(e.getVisibleInMenu(), config.visibleInMenu(), e::setVisibleInMenu);
                changed |= updateField(e.getRowCount(), config.rowCount(), e::setRowCount);
                changed |= updateField(e.getDisplayLabel(), config.displayLabel(), e::setDisplayLabel);
                changed |= updateField(e.getIcon(), config.icon(), e::setIcon);

                return changed;
        }

        private <T> boolean updateField(T current, T newValue, java.util.function.Consumer<T> setter) {
                if (!Objects.equals(current, newValue)) {
                        setter.accept(newValue);
                        return true;
                }
                return false;
        }

        private Integer calculateSortOrder(AppEntityConfig config) {
                if (Boolean.TRUE.equals(config.visibleInMenu())) {
                        return ensureUniqueSortOrder(config.sortOrder(), config.resource());
                } else {
                        return (config.sortOrder() != null) ? config.sortOrder() : computeMaxSortOrder() + 1;
                }
        }

        // ========= BOOTSTRAP AUTOMÁTICO DE ATRIBUTOS =========
        @Transactional
        protected void bootstrapEntityAttributesFromMetamodel() {
                Metamodel mm = em.getMetamodel();

                Map<String, AppEntity> byEntityName = appEntityRepository.findAll().stream()
                                .collect(Collectors.toMap(AppEntity::getEntityName, x -> x, (a, b) -> a));

                // Comenzamos a asignar sortOrder para nuevas entidades DESPUÉS del máximo
                // actual
                int nextSortOrder = computeMaxSortOrder();

                for (ManagedType<?> mt : mm.getManagedTypes()) {
                        // ---- sin pattern matching (Java 8/11) ----
                        if (!(mt instanceof jakarta.persistence.metamodel.EntityType)) {
                                continue;
                        }
                        jakarta.persistence.metamodel.EntityType<?> et = (jakarta.persistence.metamodel.EntityType<?>) mt;
                        // -----------------------------------------

                        String entityName = et.getJavaType().getSimpleName();

                        AppEntity e = byEntityName.get(entityName);
                        if (e == null) {
                                String resource = toResource(entityName);
                                e = AppEntity.builder()
                                                .resource(resource)
                                                .entityName(entityName)
                                                .className(et.getJavaType().getName())
                                                .tableName(et.getName())
                                                .idType(resolveIdType(et))
                                                .endpointBase("/" + ensurePlural(toKebab(resource)))
                                                .displayLabel(ensurePlural(entityName))
                                                .sortOrder(++nextSortOrder) // colocar al final para evitar choques
                                                .visibleInMenu(false)
                                                .createLevel(2).readLevel(4).updateLevel(3).deleteLevel(3)
                                                .build();
                                appEntityRepository.save(e);
                                byEntityName.put(entityName, e);
                        }

                        Map<String, AppEntityAttribute> existing = (e.getAttributes() == null)
                                        ? new HashMap<String, AppEntityAttribute>()
                                        : e.getAttributes().stream()
                                                        .collect(Collectors.toMap(AppEntityAttribute::getName, a -> a,
                                                                        (a, b) -> a));

                        if (e.getAttributes() == null) {
                                e.setAttributes(new ArrayList<AppEntityAttribute>());
                        }

                        int order = 0;

                        for (Attribute<?, ?> a : et.getAttributes()) {
                                String name = a.getName();

                                if (shouldSkip(name, a))
                                        continue;

                                AppEntityAttribute attr = existing.get(name);
                                if (attr == null) {
                                        attr = new AppEntityAttribute();
                                        attr.setAppEntity(e);
                                        attr.setName(name);

                                        fillTypeInfoFromMetamodel(attr, a);

                                        attr.setListVisible(defaultListVisible(name, a));
                                        attr.setListOrder(order += 1);
                                        attr.setListLabel(humanize(name));
                                        attr.setListSearchable(Boolean.TRUE);
                                        attr.setListSortable(Boolean.TRUE);

                                        // Por defecto, los campos createdAt y updatedAt no deben ser visibles en
                                        // formularios
                                        if (Set.of("createdat", "updatedat").contains(name.toLowerCase())) {
                                                attr.setFormVisible(Boolean.FALSE);
                                        } else {
                                                attr.setFormVisible(Boolean.TRUE);
                                        }
                                        attr.setFormOrder(attr.getListOrder());
                                        attr.setFormLabel(humanize(name));
                                        attr.setControlType(pickControlType(attr));

                                        e.getAttributes().add(attr);
                                } else {
                                        if (attr.getAttrType() == null || attr.getDataType() == null
                                                        || attr.getRelationTarget() == null) {
                                                fillTypeInfoFromMetamodel(attr, a);
                                        }
                                        if (attr.getListOrder() == null)
                                                attr.setListOrder(order += 1);
                                        if (attr.getListLabel() == null)
                                                attr.setListLabel(humanize(name));
                                        if (attr.getFormOrder() == null)
                                                attr.setFormOrder(attr.getListOrder());
                                        if (attr.getFormLabel() == null)
                                                attr.setFormLabel(humanize(name));
                                        if (attr.getControlType() == null)
                                                attr.setControlType(pickControlType(attr));
                                        if (attr.getListVisible() == null)
                                                attr.setListVisible(defaultListVisible(name, a));
                                        if (attr.getListSearchable() == null)
                                                attr.setListSearchable(Boolean.TRUE);
                                        if (attr.getListSortable() == null)
                                                attr.setListSortable(Boolean.TRUE);
                                }
                        }

                        appEntityRepository.save(e);
                }
        }

        private boolean shouldSkip(String name, Attribute<?, ?> a) {
                if (a instanceof PluralAttribute<?, ?, ?>)
                        return true;

                String n = name.toLowerCase(Locale.ROOT);
                if (Set.of("password", "devices", "advices", "users", "roles").contains(n))
                        return true;

                return false;
        }

        private String resolveIdType(jakarta.persistence.metamodel.EntityType<?> et) {
                try {
                        // EntityType<X> extiende IdentifiableType<X>
                        IdentifiableType<?> idt = et;
                        if (idt.getIdType() != null) {
                                Class<?> idClass = idt.getIdType().getJavaType();
                                return simpleJavaToType(idClass);
                        }
                } catch (Exception ignored) {
                }
                return "Long";
        }

        private void fillTypeInfoFromMetamodel(AppEntityAttribute attr, Attribute<?, ?> a) {
                Attribute.PersistentAttributeType pat = a.getPersistentAttributeType();

                if (pat == Attribute.PersistentAttributeType.MANY_TO_ONE
                                || pat == Attribute.PersistentAttributeType.ONE_TO_ONE) {
                        Class<?> target = a.getJavaType();
                        String simple = target.getSimpleName();
                        attr.setAttrType(simple);
                        attr.setDataType(simple);
                        attr.setRelationTarget(simple);
                        return;
                }

                Class<?> javaType = a.getJavaType();
                String t = simpleJavaToType(javaType);
                attr.setAttrType(t);
                attr.setDataType(t);
                if (attr.getRelationTarget() == null) {
                        attr.setRelationTarget(null);
                }
        }

        private boolean defaultListVisible(String name, Attribute<?, ?> a) {
                String n = name.toLowerCase(Locale.ROOT);
                if (n.equals("id"))
                        return false;
                if (Set.of("createdat", "updatedat").contains(n))
                        return false;
                if (a instanceof PluralAttribute<?, ?, ?>)
                        return false;
                if (n.equals("password"))
                        return false;
                return true;
        }

        private String pickControlType(AppEntityAttribute a) {
                if (a.getRelationTarget() != null)
                        return "select";

                String t = (a.getDataType() == null ? "" : a.getDataType()).toLowerCase(Locale.ROOT);
                if ("boolean".equals(t))
                        return "switch";
                if (Set.of("integer", "int", "long", "double", "float", "number", "bigdecimal").contains(t))
                        return "number";
                if (Set.of("localdate", "date").contains(t))
                        return "date";
                if (Set.of("localtime", "time").contains(t))
                        return "time";
                if (Set.of("localdatetime", "offsetdatetime", "instant", "datetime").contains(t))
                        return "datetime";

                String n = a.getName() == null ? "" : a.getName().toLowerCase(Locale.ROOT);
                if (n.contains("color"))
                        return "color";
                if (n.equals("email") || n.endsWith("email"))
                        return "email";
                if (n.contains("password"))
                        return "password";
                if (n.contains("url") || n.contains("link"))
                        return "url";
                if (n.contains("phone") || n.contains("mobile"))
                        return "phone";
                return "text";
        }

        private String simpleJavaToType(Class<?> c) {
                if (c == null)
                        return "String";
                if (Number.class.isAssignableFrom(c)
                                || c == int.class || c == long.class || c == double.class || c == float.class
                                || c == short.class)
                        return "Number";
                if (c == boolean.class || c == Boolean.class)
                        return "Boolean";
                if (c.getName().startsWith("java.time.")) {
                        String s = c.getSimpleName().toLowerCase(Locale.ROOT);
                        if (s.contains("date"))
                                return "Date";
                        if (s.contains("time") && !s.contains("date"))
                                return "Time";
                        return "DateTime";
                }
                if (c == String.class || CharSequence.class.isAssignableFrom(c))
                        return "String";
                return c.getSimpleName();
        }

        private String toKebab(String s) {
                if (s == null)
                        return "";
                return s.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                                .replaceAll("[\\s_]+", "-")
                                .toLowerCase(Locale.ROOT);
        }

        private String ensurePlural(String s) {
                if (s == null || s.isBlank())
                        return s;
                if (s.endsWith("s"))
                        return s;
                if (s.endsWith("y") && !s.matches(".*(ay|ey|iy|oy|uy)$"))
                        return s.substring(0, s.length() - 1) + "ies";
                return s + "s";
        }

        private String toResource(String entityName) {
                if (entityName == null)
                        return "";
                return entityName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        }

        private String humanize(String s) {
                if (s == null)
                        return "";
                String r = s.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                                .replaceAll("[-_]+", " ")
                                .replaceAll("\\s+", " ")
                                .trim();
                if (r.isEmpty())
                        return "";
                return Character.toUpperCase(r.charAt(0)) + r.substring(1);
        }

        // ========= utilidades =========
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
                        companyRepository.save(
                                        Company.builder()
                                                        .name(name)
                                                        .observations(observations)
                                                        .stripeCustomerId(null)
                                                        .stripeSubscriptionId(null)
                                                        .stripeSubscriptionItemId(null)
                                                        .billingStatus(Company.BillingStatus.INCOMPLETE)
                                                        .build());
                }
        }

        private void createDefaultAdminUser(String username, String email, String rawPassword,
                        String name, String lastName, Role adminRole) {
                if (userRepository.existsByUsername(username)) {
                        log.info("ℹ️  Usuario admin ya existe: {}", username);
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
                log.info("✅ Usuario admin creado: {} / {}", username, email);
        }
}
