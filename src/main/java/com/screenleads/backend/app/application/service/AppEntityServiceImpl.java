package com.screenleads.backend.app.application.service;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.repositories.AppEntityRepository;
import com.screenleads.backend.app.web.dto.AppEntityDTO;

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

    @Override
    public List<AppEntityDTO> findAll(boolean withCount) {
        List<AppEntity> all = repo.findAll();
        if (withCount) {
            refreshRowCountsInMemory(all);
        }
        // ordenar por sortOrder si existe, luego por displayLabel
        all.sort((a, b) -> {
            int sa = a.getSortOrder() == null ? Integer.MAX_VALUE : a.getSortOrder();
            int sb = b.getSortOrder() == null ? Integer.MAX_VALUE : b.getSortOrder();
            if (sa != sb) return Integer.compare(sa, sb);
            String la = a.getDisplayLabel() != null ? a.getDisplayLabel() : a.getEntityName();
            String lb = b.getDisplayLabel() != null ? b.getDisplayLabel() : b.getEntityName();
            return la.compareToIgnoreCase(lb);
        });
        return all.stream().map(this::toDto).toList();
    }

    @Override
    public AppEntityDTO findById(Long id, boolean withCount) {
        AppEntity e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AppEntity no encontrada: id=" + id));
        if (withCount) {
            e.setRowCount(countRowsSafe(e.getTableName()));
        }
        return toDto(e);
    }

    @Override
    public AppEntityDTO findByResource(String resource, boolean withCount) {
        AppEntity e = repo.findByResource(resource)
                .orElseThrow(() -> new IllegalArgumentException("AppEntity no encontrada: resource=" + resource));
        if (withCount) {
            e.setRowCount(countRowsSafe(e.getTableName()));
        }
        return toDto(e);
    }

    @Override
    @Transactional
    public AppEntityDTO upsert(AppEntityDTO dto) {
        AppEntity e = repo.findByResource(dto.getResource())
                .orElseGet(() -> AppEntity.builder().resource(dto.getResource()).build());

        e.setEntityName(dto.getEntityName());
        e.setClassName(dto.getClassName());
        e.setTableName(dto.getTableName());
        e.setIdType(dto.getIdType());
        e.setEndpointBase(dto.getEndpointBase());
        e.setCreateLevel(dto.getCreateLevel());
        e.setReadLevel(dto.getReadLevel());
        e.setUpdateLevel(dto.getUpdateLevel());
        e.setDeleteLevel(dto.getDeleteLevel());

        Map<String, String> attrs = dto.getAttributes() != null
                ? new LinkedHashMap<>(dto.getAttributes())
                : new LinkedHashMap<>();
        e.setAttributes(attrs);

        // Dashboard metadata
        e.setDisplayLabel(dto.getDisplayLabel() != null ? dto.getDisplayLabel()
                : (dto.getEntityName() != null ? dto.getEntityName() : e.getDisplayLabel()));
        e.setIcon(dto.getIcon());
        e.setSortOrder(dto.getSortOrder());

        AppEntity saved = repo.save(e);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    // ---- helpers row count ----

    private void refreshRowCountsInMemory(List<AppEntity> entities) {
        for (AppEntity e : entities) {
            e.setRowCount(countRowsSafe(e.getTableName()));
        }
    }

    private Long countRowsSafe(String tableName) {
        if (tableName == null || tableName.isBlank() || jdbcTemplate == null) return null;
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        } catch (Exception ex) {
            return null;
        }
    }

    // ---- mapper ----

    private AppEntityDTO toDto(AppEntity e) {
        return AppEntityDTO.builder()
            .id(e.getId())
            .resource(e.getResource())
            .entityName(e.getEntityName())
            .className(e.getClassName())
            .tableName(e.getTableName())
            .idType(e.getIdType())
            .endpointBase(e.getEndpointBase())
            .createLevel(e.getCreateLevel())
            .readLevel(e.getReadLevel())
            .updateLevel(e.getUpdateLevel())
            .deleteLevel(e.getDeleteLevel())
            .rowCount(e.getRowCount())
            .attributes(e.getAttributes() != null ? e.getAttributes() : new LinkedHashMap<>())
            .displayLabel(e.getDisplayLabel() != null ? e.getDisplayLabel() : e.getEntityName())
            .icon(e.getIcon())
            .sortOrder(e.getSortOrder())
            .build();
    }
}
