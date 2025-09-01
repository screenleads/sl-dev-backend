package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.web.dto.EntityInfo;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Lista metadata de entidades con filtro:
 * - Deben tener repositorio (CRUD) registrado.
 * - El usuario actual debe tener permiso de edición (create/update/delete)
 * sobre la entidad.
 */
@Service
public class MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    private final Repositories repositories;
    private final PermissionService perm; // <- tu servicio de permisos

    // Ajusta los paquetes base si lo necesitas
    private static final List<String> BASE_PACKAGES = Arrays.asList("com.screenleads", "com.sl");

    public MetadataService(ApplicationContext applicationContext, PermissionService permissionService) {
        this.repositories = new Repositories(applicationContext);
        this.perm = permissionService;
    }

    public List<EntityInfo> getAllEntities(boolean withCount) {
        List<EntityInfo> list = new ArrayList<>();

        // 1) Intentar obtener domainTypes desde los repositorios
        Set<Class<?>> domainTypes = getRepositoryDomainTypes();

        // 2) Fallback: escanear @Entity en el classpath si no se encontró nada
        if (domainTypes.isEmpty()) {
            domainTypes.addAll(scanEntitiesOnClasspath());
            log.info("Metadata fallback: encontradas {} entidades via escaneo @Entity", domainTypes.size());
        } else {
            log.info("Metadata: encontradas {} entidades via Repositories", domainTypes.size());
        }

        for (Class<?> domainType : domainTypes) {
            try {
                final String fqn = domainType.getName();
                if (!startsWithAny(fqn, BASE_PACKAGES))
                    continue;

                // Debe existir un repositorio CRUD para ser editable en la app
                if (repositories.getRepositoryFor(domainType).isEmpty()) {
                    log.debug("Entidad {} ignorada (sin repositorio asociado)", fqn);
                    continue;
                }

                final String entityName = domainType.getSimpleName();
                final String permKey = normalizeEntityKey(entityName);

                // ---- FILTRO DE PERMISOS (tu PermissionService) ----
                // Editable si el usuario puede create/update/delete la entidad
                boolean canEdit = perm.can(permKey, "create")
                        || perm.can(permKey, "update")
                        || perm.can(permKey, "delete");
                if (!canEdit) {
                    log.debug("Entidad {} ignorada (usuario sin permiso de edición)", entityName);
                    continue;
                }
                // ---------------------------------------------------

                final String tableName = Optional.ofNullable(domainType.getAnnotation(Table.class))
                        .map(Table::name)
                        .filter(s -> !s.isBlank())
                        .orElse(null);

                final String idType = resolveIdType(domainType);

                // Atributos básicos (fields no estáticos ni transient; incluye heredados)
                Map<String, String> attributes = new LinkedHashMap<>();
                for (Field f : getAllFields(domainType)) {
                    if (Modifier.isStatic(f.getModifiers()))
                        continue;
                    if (Modifier.isTransient(f.getModifiers()))
                        continue;
                    attributes.put(f.getName(), f.getType().getSimpleName());
                }

                Long rowCount = null;
                if (withCount) {
                    // Marcador seguro para no tocar la BD (si quieres contar de verdad, descomenta
                    // abajo)
                    rowCount = -1L;

                    // repositories.getRepositoryFor(domainType).ifPresent(repo -> {
                    // if (repo instanceof org.springframework.data.repository.CrudRepository<?, ?>
                    // cr) {
                    // try { rowCount = cr.count(); } catch (Exception e) { rowCount = -1L; }
                    // }
                    // });
                }

                list.add(new EntityInfo(entityName, fqn, tableName, idType, attributes, rowCount));

            } catch (Exception ex) {
                log.warn("No se pudo construir metadata de {}", domainType, ex);
            }
        }

        list.sort(Comparator.comparing(EntityInfo::getEntityName));
        return list;
    }

    // ----------------- Helpers -----------------

    /**
     * Normaliza el nombre simple de la entidad a la clave que usa tu
     * PermissionService (lowercase).
     */
    private String normalizeEntityKey(String simpleName) {
        // Casos típicos: User -> "user", DeviceType -> "devicetype", AppVersion ->
        // "appversion"
        // Si manejas sufijos como "Entity" en tus clases, elimínalos:
        String s = simpleName;
        if (s.endsWith("Entity")) {
            s = s.substring(0, s.length() - "Entity".length());
        }
        return s.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Intenta obtener los domain types de los repos (compatible con múltiples
     * versiones de Spring Data).
     */
    private Set<Class<?>> getRepositoryDomainTypes() {
        Set<Class<?>> types = new LinkedHashSet<>();

        // A) Intento directo: Repositories implementa Iterable<Class<?>>
        try {
            for (Class<?> c : repositories) {
                types.add(c);
            }
        } catch (Throwable t) {
            log.debug("Repositories no es iterable directamente en esta versión: {}", t.toString());
        }

        // B) Intento por reflexión: método getDomainTypes() (existe en versiones
        // nuevas)
        try {
            Method m = repositories.getClass().getMethod("getDomainTypes");
            Object obj = m.invoke(repositories);
            if (obj instanceof Iterable<?>) {
                for (Object o : (Iterable<?>) obj) {
                    if (o instanceof Class<?>) {
                        types.add((Class<?>) o);
                    }
                }
            }
        } catch (NoSuchMethodException ignore) {
            // método no existe en esta versión → OK
        } catch (Exception ex) {
            log.debug("Error invocando getDomainTypes() por reflexión: {}", ex.toString());
        }

        return types;
    }

    /**
     * Escanea el classpath en busca de clases anotadas con @Entity dentro de
     * BASE_PACKAGES.
     */
    private Set<Class<?>> scanEntitiesOnClasspath() {
        Set<Class<?>> result = new LinkedHashSet<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        for (String basePackage : BASE_PACKAGES) {
            try {
                for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
                    String className = bd.getBeanClassName();
                    if (className == null)
                        continue;
                    try {
                        Class<?> clazz = Class.forName(className);
                        result.add(clazz);
                    } catch (ClassNotFoundException e) {
                        log.warn("No se pudo cargar clase escaneada: {}", className);
                    }
                }
            } catch (Exception e) {
                log.warn("Fallo escaneando paquete {}: {}", basePackage, e.toString());
            }
        }
        return result;
    }

    private boolean startsWithAny(String fqn, List<String> prefixes) {
        for (String p : prefixes) {
            if (fqn.startsWith(p + "."))
                return true;
        }
        return false;
    }

    private String resolveIdType(Class<?> domainType) {
        for (Field f : getAllFields(domainType)) {
            if (f.isAnnotationPresent(Id.class)) {
                return f.getType().getSimpleName();
            }
            if (f.isAnnotationPresent(EmbeddedId.class)) {
                return f.getType().getSimpleName() + " (EmbeddedId)";
            }
        }
        return "UnknownId";
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
}
