package com.screenleads.backend.app.web.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.AppEntityAttribute;
import com.screenleads.backend.app.web.dto.AppEntityDTO;
import com.screenleads.backend.app.web.dto.EntityAttributeDTO;

public final class AppEntityMapper {

    private AppEntityMapper() {
    }

    public static AppEntityDTO toDto(AppEntity e) {
        List<EntityAttributeDTO> attrsDto = (e.getAttributes() == null)
                ? List.of()
                : e.getAttributes().stream()
                        .map(AppEntityMapper::toDto)
                        .sorted(Comparator.comparing(a -> Optional.ofNullable(a.listOrder()).orElse(0)))
                        .toList();

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
                .visibleInMenu(e.getVisibleInMenu())
                .rowCount(e.getRowCount())
                .displayLabel(e.getDisplayLabel())
                .icon(e.getIcon())
                .sortOrder(e.getSortOrder())
                .attributes(attrsDto)
                .build();
    }

    private static EntityAttributeDTO toDto(AppEntityAttribute a) {
        return EntityAttributeDTO.builder()
                .id(a.getId())
                .name(a.getName())
                .attrType(a.getAttrType())
                .dataType(a.getDataType())
                .relationTarget(a.getRelationTarget())
                .listLabel(firstNonBlank(a.getListLabel(), humanize(a.getName())))
                .listVisible(defaultIfNull(a.getListVisible(), Boolean.TRUE))
                .listOrder(a.getListOrder())
                .formLabel(firstNonBlank(a.getFormLabel(), humanize(a.getName())))
                .formOrder(a.getFormOrder() != null ? a.getFormOrder() : a.getListOrder())
                .controlType(a.getControlType())
                .listSearchable(defaultIfNull(a.getListSearchable(), Boolean.TRUE))
                .listSortable(defaultIfNull(a.getListSortable(), Boolean.TRUE))
                .build();
    }

    private static <T> T defaultIfNull(T val, T def) {
        return val != null ? val : def;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private static String humanize(String s) {
        if (s == null)
            return "";
        String t = s.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("[-_]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (t.isEmpty())
            return t;
        return t.substring(0, 1).toUpperCase() + t.substring(1);
    }
}
