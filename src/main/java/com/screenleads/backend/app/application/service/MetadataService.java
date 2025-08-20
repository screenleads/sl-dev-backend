// src/main/java/com/screenleads/backend/app/service/MetadataService.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.web.dto.EntityInfo;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@Service
public class MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    private final Repositories repositories;

    public MetadataService(ApplicationContext applicationContext) {
        // Explora todos los repositorios Spring Data del contexto
        this.repositories = new Repositories(applicationContext);
    }

    /**
     * Lista entidades a partir de los repositorios registrados.
     * No ejecuta ninguna query ni toca el metamodel de JPA.
     */
    public List<EntityInfo> getAllEntities(boolean withCount) {
        List<EntityInfo> list = new ArrayList<>();

        for (Class<?> domainType : repositories) {
            try {
                // Filtra solo tus paquetes (ajusta prefijos si conviene)
                String fqn = domainType.getName();
                if (!fqn.startsWith("com.screenleads.") && !fqn.startsWith("com.sl."))
                    continue;

                String entityName = domainType.getSimpleName();

                // @Table(name=...) si existe
                String tableName = Optional.ofNullable(domainType.getAnnotation(Table.class))
                        .map(Table::name)
                        .filter(s -> !s.isBlank())
                        .orElse(null);

                // idType buscando @Id o @EmbeddedId en fields
                String idType = resolveIdType(domainType);

                // atributos públicos ÚTILES (por simplicidad: fields declarados, no estáticos,
                // no transient)
                Map<String, String> attributes = new LinkedHashMap<>();
                for (Field f : getAllFields(domainType)) {
                    if (Modifier.isStatic(f.getModifiers()))
                        continue;
                    if (Modifier.isTransient(f.getModifiers()))
                        continue;
                    f.setAccessible(true);
                    attributes.put(f.getName(), f.getType().getSimpleName());
                }

                // Por defecto no contamos (para evitar toques a DB y problemas de filtros)
                Long rowCount = null;
                if (withCount) {
                    // Sugerencia: solo si quieres, intenta contar usando el repo asociado
                    // var repo = repositories.getRepositoryFor(domainType).orElse(null);
                    // if (repo instanceof org.springframework.data.repository.CrudRepository<?, ?>
                    // cr) {
                    // rowCount = cr.count(); // ¡Esto sí toca DB! Úsalo si estás seguro
                    // } else {
                    // rowCount = -1L;
                    // }
                    rowCount = -1L; // Placeholder seguro
                }

                list.add(new EntityInfo(entityName, fqn, tableName, idType, attributes, rowCount));

            } catch (Exception ex) {
                log.warn("No se pudo construir metadata de {}", domainType, ex);
            }
        }

        list.sort(Comparator.comparing(EntityInfo::getEntityName));
        return list;
    }

    private String resolveIdType(Class<?> domainType) {
        for (Field f : getAllFields(domainType)) {
            if (f.isAnnotationPresent(Id.class)) {
                return f.getType().getSimpleName();
            }
        }
        // Si usas @EmbeddedId o no encuentras @Id, devuelve un marcador
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
