package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.web.dto.EntityInfo;
import jakarta.persistence.EmbeddedId;
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
        this.repositories = new Repositories(applicationContext);
    }

    public List<EntityInfo> getAllEntities(boolean withCount) {
        List<EntityInfo> list = new ArrayList<>();

        // ✅ Iteración compatible con todas las versiones: Repositories implements
        // Iterable<Class<?>>
        for (Class<?> domainType : repositories) {
            try {
                final String fqn = domainType.getName();
                // Ajusta estos prefijos a tus paquetes
                if (!fqn.startsWith("com.screenleads.") && !fqn.startsWith("com.sl.")) {
                    continue;
                }

                final String entityName = domainType.getSimpleName();

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
                    // Marcador seguro para no tocar la BD
                    rowCount = -1L;

                    // Si quieres contar de verdad (esto SÍ hace query), descomenta:
                    // Optional<Object> repoOpt = repositories.getRepositoryFor(domainType);
                    // if (repoOpt.isPresent() && repoOpt.get() instanceof
                    // org.springframework.data.repository.CrudRepository<?, ?> cr) {
                    // try { rowCount = cr.count(); } catch (Exception e) { rowCount = -1L; }
                    // }
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
