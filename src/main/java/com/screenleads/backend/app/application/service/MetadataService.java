package com.screenleads.backend.app.application.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.web.dto.EntityInfo;

import java.util.*;

@Service
public class MetadataService {

    private final EntityManager em;

    public MetadataService(EntityManager em) {
        this.em = em;
    }

    /**
     * Obtiene la lista de entidades JPA registradas en el Metamodel con metadatos.
     * 
     * @param withCount si true, calcula COUNT(*) por entidad
     */
    @Transactional(readOnly = true)
    public List<EntityInfo> getAllEntities(boolean withCount) {
        Metamodel metamodel = em.getMetamodel();
        Set<EntityType<?>> entities = metamodel.getEntities();

        List<EntityInfo> list = new ArrayList<>();
        for (EntityType<?> et : entities) {
            // Nombre simple de la clase
            Class<?> javaType = et.getJavaType();
            String className = javaType.getName();
            String entityName = javaType.getSimpleName();

            // Table name (si tiene @Table; si no, null)
            String tableName = Optional.ofNullable(javaType.getAnnotation(Table.class))
                    .map(Table::name)
                    .filter(s -> !s.isBlank())
                    .orElse(null);

            // Tipo de ID (si lo conoce el metamodel)
            String idType = null;
            try {
                if (et.getIdType() != null && et.getIdType().getJavaType() != null) {
                    idType = et.getIdType().getJavaType().getSimpleName();
                }
            } catch (IllegalArgumentException ignored) {
                /* composite IDs u otros casos */ }

            // Atributos: nombre -> tipo simple
            Map<String, String> attributes = new LinkedHashMap<>();
            for (Attribute<?, ?> attr : et.getAttributes()) {
                Class<?> attrType = attr.getJavaType();
                attributes.put(attr.getName(), attrType != null ? attrType.getSimpleName() : "Object");
            }

            // COUNT(*) opcional vía Criteria API
            Long rowCount = null;
            if (withCount) {
                try {
                    var cb = em.getCriteriaBuilder();
                    var cq = cb.createQuery(Long.class);
                    var root = cq.from(javaType);
                    cq.select(cb.count(root));
                    rowCount = em.createQuery(cq).getSingleResult();
                } catch (Exception e) {
                    // Evitamos romper el endpoint si alguna entidad falla (vistas, permisos, etc.)
                    rowCount = -1L;
                }
            }

            list.add(new EntityInfo(entityName, className, tableName, idType, attributes, rowCount));
        }

        // Orden estable: por nombre de entidad
        list.sort(Comparator.comparing(EntityInfo::getEntityName));
        return list;
    }
}
