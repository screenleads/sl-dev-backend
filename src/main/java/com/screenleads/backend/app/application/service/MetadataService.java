// src/main/java/com/sl/dev/backend/services/MetadataService.java
package com.screenleads.backend.app.application.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.web.dto.EntityInfo;

import java.util.*;

@Service
public class MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);
    private final EntityManager em;

    public MetadataService(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    public List<EntityInfo> getAllEntities(boolean withCount) {
        Metamodel metamodel = em.getMetamodel();
        Set<EntityType<?>> entities = metamodel.getEntities();

        List<EntityInfo> list = new ArrayList<>();
        for (EntityType<?> et : entities) {
            try {
                Class<?> javaType = et.getJavaType();
                if (javaType == null)
                    continue;

                // (Opcional) filtra cualquier clase fuera de tu paquete
                String fqn = javaType.getName();
                if (!fqn.startsWith("com.screenleads.") && !fqn.startsWith("com.sl.")) {
                    // evita clases internas de Hibernate o libs
                    continue;
                }

                String entityName = javaType.getSimpleName();
                String className = fqn;

                String tableName = Optional.ofNullable(javaType.getAnnotation(Table.class))
                        .map(Table::name)
                        .filter(s -> !s.isBlank())
                        .orElse(null);

                String idType = null;
                try {
                    if (et.getIdType() != null && et.getIdType().getJavaType() != null) {
                        idType = et.getIdType().getJavaType().getSimpleName();
                    }
                } catch (IllegalArgumentException ex) {
                    // IDs compuestas u otros casos
                    idType = "CompositeId";
                } catch (Exception ex) {
                    log.warn("No se pudo resolver idType para {}", entityName, ex);
                }

                Map<String, String> attributes = new LinkedHashMap<>();
                for (Attribute<?, ?> attr : et.getAttributes()) {
                    try {
                        String aName = attr.getName();
                        Class<?> aType = attr.getJavaType();
                        attributes.put(aName, aType != null ? aType.getSimpleName() : "Object");
                    } catch (Exception ex) {
                        // sigue si un atributo concreto falla
                        log.warn("No se pudo resolver atributo en {}: {}", entityName, attr, ex);
                    }
                }

                Long rowCount = null;
                if (withCount) {
                    try {
                        var cb = em.getCriteriaBuilder();
                        var cq = cb.createQuery(Long.class);
                        cq.select(cb.count(cq.from(javaType)));
                        rowCount = em.createQuery(cq).getSingleResult();
                    } catch (Exception ex) {
                        // no rompas el endpoint por conteo de una entidad
                        log.warn("COUNT falló en entidad {}: {}", entityName, ex.getMessage());
                        rowCount = -1L;
                    }
                }

                list.add(new EntityInfo(entityName, className, tableName, idType, attributes, rowCount));

            } catch (Exception e) {
                // Nunca tumbes la respuesta por una entidad problemática
                log.error("Fallo recopilando metadatos de entidad {}: {}", safeName(et), e.toString());
            }
        }

        list.sort(Comparator.comparing(EntityInfo::getEntityName));
        return list;
    }

    private String safeName(EntityType<?> et) {
        try {
            return et.getJavaType() != null ? et.getJavaType().getName() : "<unknown>";
        } catch (Exception ignored) {
            return "<unknown>";
        }
    }
}