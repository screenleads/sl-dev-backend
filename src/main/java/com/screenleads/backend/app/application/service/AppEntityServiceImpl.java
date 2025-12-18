package com.screenleads.backend.app.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.AppEntityAttribute;
import com.screenleads.backend.app.domain.repositories.AppEntityRepository;
import com.screenleads.backend.app.web.dto.AppEntityDTO;
import com.screenleads.backend.app.web.dto.EntityAttributeDTO;
import com.screenleads.backend.app.web.mapper.AppEntityMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppEntityServiceImpl implements AppEntityService {

    private static final String APP_ENTITY_NOT_FOUND = "AppEntity no encontrada: ";

    private final AppEntityRepository repo;

    @Nullable
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(@Nullable JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ===================== QUERY =====================

    @Override
    public List<AppEntityDTO> findAll(boolean withCount) {
        List<AppEntity> all = repo.findAll();

        if (withCount) {
            refreshRowCountsInMemory(all);
        }

        // ordenar por sortOrder si existe, luego por displayLabel/entityName
        all.sort((a, b) -> {
            int sa = a.getSortOrder() == null ? Integer.MAX_VALUE : a.getSortOrder();
            int sb = b.getSortOrder() == null ? Integer.MAX_VALUE : b.getSortOrder();
            if (sa != sb)
                return Integer.compare(sa, sb);
            String la = a.getDisplayLabel() != null ? a.getDisplayLabel() : a.getEntityName();
            String lb = b.getDisplayLabel() != null ? b.getDisplayLabel() : b.getEntityName();
            return la.compareToIgnoreCase(lb);
        });

        return all.stream().map(AppEntityMapper::toDto).toList();
    }

    @Override
    public AppEntityDTO findById(Long id, boolean withCount) {
        AppEntity e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(APP_ENTITY_NOT_FOUND + "id=" + id));
        if (withCount) {
            e.setRowCount(countRowsSafe(e.getTableName()));
        }
        return AppEntityMapper.toDto(e);
    }

    @Override
    public AppEntityDTO findByResource(String resource, boolean withCount) {
        AppEntity e = repo.findByResource(resource)
                .orElseThrow(() -> new IllegalArgumentException(APP_ENTITY_NOT_FOUND + "resource=" + resource));
        if (withCount) {
            e.setRowCount(countRowsSafe(e.getTableName()));
        }
        return AppEntityMapper.toDto(e);
    }

    // ===================== COMMANDS =====================

    @Override
    @Transactional
    public AppEntityDTO upsert(AppEntityDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AppEntityDTO nulo");
        }
        if (dto.resource() == null || dto.resource().isBlank()) {
            throw new IllegalArgumentException("resource obligatorio en AppEntityDTO");
        }

        AppEntity e;
        if (dto.id() != null) {
            e = repo.findById(dto.id()).orElseGet(() -> AppEntity.builder().id(dto.id()).resource(dto.resource()).build());
        } else {
            e = repo.findByResource(dto.resource()).orElseGet(() -> AppEntity.builder().resource(dto.resource()).build());
        }

        // Metadatos principales
        e.setEntityName(nullIfBlank(dto.entityName(), e.getEntityName()));
        e.setClassName(nullIfBlank(dto.className(), e.getClassName()));
        e.setTableName(nullIfBlank(dto.tableName(), e.getTableName()));
        e.setIdType(nullIfBlank(dto.idType(), e.getIdType()));
        e.setEndpointBase(nullIfBlank(dto.endpointBase(), e.getEndpointBase()));
        e.setCreateLevel(dto.createLevel() != null ? dto.createLevel() : e.getCreateLevel());
        e.setReadLevel(dto.readLevel() != null ? dto.readLevel() : e.getReadLevel());
        e.setUpdateLevel(dto.updateLevel() != null ? dto.updateLevel() : e.getUpdateLevel());
        e.setDeleteLevel(dto.deleteLevel() != null ? dto.deleteLevel() : e.getDeleteLevel());
        e.setVisibleInMenu(dto.visibleInMenu() != null ? dto.visibleInMenu() : e.getVisibleInMenu());
        // Dashboard metadata
        if (dto.displayLabel() != null && !dto.displayLabel().isBlank()) {
            e.setDisplayLabel(dto.displayLabel());
        } else if (e.getDisplayLabel() == null && e.getEntityName() != null) {
            e.setDisplayLabel(e.getEntityName());
        }
        e.setIcon(dto.icon() != null ? dto.icon() : e.getIcon());
        e.setSortOrder(dto.sortOrder() != null ? dto.sortOrder() : e.getSortOrder());

        // Atributos (merge no destructivo)
        if (dto.attributes() != null) {
            mergeAttributes(e, dto.attributes());
        }

        AppEntity saved = repo.save(e);
        return AppEntityMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    // ===================== REORDER (drag & drop) =====================

    @Override
    @Transactional
    public void reorderEntities(List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("Debe enviarse una lista de IDs para reordenar.");
        }

        // Validar duplicados en la lista
        var seen = new HashSet<Long>();
        for (Long id : orderedIds) {
            if (id == null) {
                throw new IllegalArgumentException("La lista de IDs no puede contener nulos.");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("La lista de IDs contiene duplicados: " + id);
            }
        }

        // Reordenamos SOLO las visibles en menú
        List<AppEntity> visibles = repo.findByVisibleInMenuTrueOrderBySortOrderAsc();
        if (visibles.isEmpty())
            return;

        Map<Long, AppEntity> byId = new HashMap<>();
        for (AppEntity e : visibles) {
            byId.put(e.getId(), e);
        }

        // Validar que todos los IDs pertenezcan al conjunto visible
        for (Long id : orderedIds) {
            if (!byId.containsKey(id)) {
                throw new IllegalArgumentException("El ID " + id + " no pertenece a entidades visibles en menú.");
            }
        }

        int order = 1;
        // Asignar primero los recibidos en el nuevo orden
        for (Long id : orderedIds) {
            AppEntity e = byId.remove(id);
            if (e != null)
                e.setSortOrder(order++);
        }

        // Mantener el resto con su orden relativo, colocándolos a continuación
        for (AppEntity e : visibles) {
            if (byId.containsKey(e.getId())) {
                e.setSortOrder(order++);
            }
        }

        repo.saveAll(visibles);
    }

    @Override
    @Transactional
    public void reorderAttributes(Long entityId, List<Long> orderedAttributeIds) {
        if (entityId == null)
            throw new IllegalArgumentException("entityId requerido");
        if (orderedAttributeIds == null || orderedAttributeIds.isEmpty()) {
            throw new IllegalArgumentException("Debe enviarse una lista de IDs de atributos para reordenar.");
        }

        var seen = new HashSet<Long>();
        for (Long id : orderedAttributeIds) {
            if (id == null) {
                throw new IllegalArgumentException("La lista de IDs de atributos no puede contener nulos.");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("La lista de IDs de atributos contiene duplicados: " + id);
            }
        }

        AppEntity entity = repo.findWithAttributesById(entityId)
                .orElseThrow(() -> new IllegalArgumentException(APP_ENTITY_NOT_FOUND + "id=" + entityId));

        if (entity.getAttributes() == null || entity.getAttributes().isEmpty())
            return;

        Map<Long, AppEntityAttribute> byId = new HashMap<>();
        for (AppEntityAttribute a : entity.getAttributes()) {
            if (a.getId() != null)
                byId.put(a.getId(), a);
        }

        // Validar pertenencia
        for (Long id : orderedAttributeIds) {
            if (!byId.containsKey(id)) {
                throw new IllegalArgumentException("El atributo " + id + " no pertenece a la entidad " + entityId);
            }
        }

        int order = 0;

        // Reordena los indicados primero
        for (Long id : orderedAttributeIds) {
            AppEntityAttribute a = byId.remove(id);
            if (a != null) {
                int newOrder = ++order;
                Integer oldListOrder = a.getListOrder();
                a.setListOrder(newOrder);

                // Solo sincroniza formOrder si no estaba personalizado
                if (a.getFormOrder() == null || (oldListOrder != null && a.getFormOrder().equals(oldListOrder))) {
                    a.setFormOrder(newOrder);
                }
            }
        }

        // Añade el resto manteniendo su orden relativo original
        List<AppEntityAttribute> remaining = entity.getAttributes().stream()
                .filter(a -> byId.containsKey(a.getId()))
                .sorted((x, y) -> {
                    Integer lx = x.getListOrder() == null ? Integer.MAX_VALUE : x.getListOrder();
                    Integer ly = y.getListOrder() == null ? Integer.MAX_VALUE : y.getListOrder();
                    return Integer.compare(lx, ly);
                })
                .toList();

        for (AppEntityAttribute a : remaining) {
            int newOrder = ++order;
            Integer oldListOrder = a.getListOrder();
            a.setListOrder(newOrder);
            if (a.getFormOrder() == null || (oldListOrder != null && a.getFormOrder().equals(oldListOrder))) {
                a.setFormOrder(newOrder);
            }
        }

        // Guardar. (Cascade en AppEntity -> AppEntityAttribute)
        repo.save(entity);
    }

    // ===================== ROW COUNT =====================

    private void refreshRowCountsInMemory(List<AppEntity> entities) {
        for (AppEntity e : entities) {
            e.setRowCount(countRowsSafe(e.getTableName()));
        }
    }

    private Long countRowsSafe(String tableName) {
        if (tableName == null || tableName.isBlank() || jdbcTemplate == null)
            return null;
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        } catch (Exception ex) {
            return null;
        }
    }

    // ===================== ATTRIBUTES MERGE/REPLACE =====================

    /**
     * Merge NO destructivo: actualiza los existentes (por id o name) y crea nuevos.
     * No borra.
     */
    private void mergeAttributes(AppEntity e, List<EntityAttributeDTO> incoming) {
        if (e.getAttributes() == null) {
            e.setAttributes(new ArrayList<>());
        }

        // Índices para localizar rápido existentes
        Map<Long, AppEntityAttribute> byId = new HashMap<>();
        Map<String, AppEntityAttribute> byName = new HashMap<>();
        for (AppEntityAttribute a : e.getAttributes()) {
            if (a.getId() != null)
                byId.put(a.getId(), a);
            if (a.getName() != null)
                byName.put(a.getName(), a);
        }

        for (EntityAttributeDTO dto : incoming) {
            AppEntityAttribute target = null;

            if (dto.id() != null) {
                target = byId.get(dto.id());
            }
            if (target == null && dto.name() != null) {
                target = byName.get(dto.name());
            }

            if (target == null) {
                // crear nuevo
                target = new AppEntityAttribute();
                target.setAppEntity(e);
                applyAttrDto(target, dto);
                e.getAttributes().add(target);
            } else {
                // actualizar existente (campos null no pisan)
                applyAttrDto(target, dto);
            }
        }
    }

    /**
     * Reemplazo exacto: deja exactamente los atributos recibidos (borra los demás).
     */
    @SuppressWarnings("unused")
    private void replaceAttributes(AppEntity e, List<EntityAttributeDTO> incoming) {
        if (e.getAttributes() == null) {
            e.setAttributes(new ArrayList<>());
        } else {
            e.getAttributes().clear();
        }
        for (EntityAttributeDTO dto : incoming) {
            AppEntityAttribute a = new AppEntityAttribute();
            a.setAppEntity(e);
            applyAttrDto(a, dto);
            e.getAttributes().add(a);
        }
    }

    /** Copia segura: sólo pisa si el DTO trae valor no nulo. */
    private void applyAttrDto(AppEntityAttribute a, EntityAttributeDTO d) {
        AppEntityMapper.applyAttrDto(a, d);
    }

    private static String nullIfBlank(String v, String fallback) {
        if (v == null)
            return fallback;
        String t = v.trim();
        return t.isEmpty() ? fallback : t;
    }
}
