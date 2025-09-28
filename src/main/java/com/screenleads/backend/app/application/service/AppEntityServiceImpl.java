package com.screenleads.backend.app.application.service;

import java.util.ArrayList;
import java.util.HashMap;
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
                .orElseThrow(() -> new IllegalArgumentException("AppEntity no encontrada: id=" + id));
        if (withCount) {
            e.setRowCount(countRowsSafe(e.getTableName()));
        }
        return AppEntityMapper.toDto(e);
    }

    @Override
    public AppEntityDTO findByResource(String resource, boolean withCount) {
        AppEntity e = repo.findByResource(resource)
                .orElseThrow(() -> new IllegalArgumentException("AppEntity no encontrada: resource=" + resource));
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

        AppEntity e = repo.findByResource(dto.resource())
                .orElseGet(() -> AppEntity.builder().resource(dto.resource()).build());

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
        // Si prefieres "replace exacto" (borrar los que no vienen), usa:
        // replaceAttributes(e, dto.attributes());

        AppEntity saved = repo.save(e);
        return AppEntityMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
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
            // WARNING: tableName viene de tu propio catálogo; si algún día es editable por
            // usuario,
            // parametriza o valida para evitar SQL injection.
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
        if (d == null)
            return;

        if (d.name() != null)
            a.setName(d.name());
        if (d.attrType() != null)
            a.setAttrType(d.attrType());
        if (d.dataType() != null)
            a.setDataType(d.dataType());
        if (d.relationTarget() != null)
            a.setRelationTarget(d.relationTarget());

        if (d.listLabel() != null)
            a.setListLabel(d.listLabel());
        if (d.listVisible() != null)
            a.setListVisible(d.listVisible());
        if (d.listOrder() != null)
            a.setListOrder(d.listOrder());

        if (d.formLabel() != null)
            a.setFormLabel(d.formLabel());
        if (d.formOrder() != null)
            a.setFormOrder(d.formOrder());
        if (d.controlType() != null)
            a.setControlType(d.controlType());

        if (d.listSearchable() != null)
            a.setListSearchable(d.listSearchable());
        if (d.listSortable() != null)
            a.setListSortable(d.listSortable());
    }

    private static String nullIfBlank(String v, String fallback) {
        if (v == null)
            return fallback;
        String t = v.trim();
        return t.isEmpty() ? fallback : t;
    }
}
