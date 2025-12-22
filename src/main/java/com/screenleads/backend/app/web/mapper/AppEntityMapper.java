// src/main/java/com/screenleads/backend/app/web/mapper/AppEntityMapper.java
package com.screenleads.backend.app.web.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.AppEntityAttribute;
import com.screenleads.backend.app.web.dto.AppEntityDTO;
import com.screenleads.backend.app.web.dto.EntityAttributeDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AppEntityMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AppEntityMapper() {
    }

    // -------- AppEntity -> DTO --------
    public static AppEntityDTO toDto(AppEntity e) {
        List<EntityAttributeDTO> attrs = new ArrayList<>();
        if (e.getAttributes() != null) {
            e.getAttributes().stream()
                    .sorted(Comparator.comparing(a -> a.getListOrder() == null ? Integer.MAX_VALUE : a.getListOrder()))
                    .map(AppEntityMapper::toDto)
                    .forEach(attrs::add);
        }

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
                .attributes(attrs)
                .build();
    }

    // -------- AppEntityAttribute -> DTO --------
    public static EntityAttributeDTO toDto(AppEntityAttribute a) {
        String enumJson = null;
        if (a.getEnumValues() != null) {
            try {
                enumJson = MAPPER.writeValueAsString(a.getEnumValues());
            } catch (Exception ignore) {
            }
        }

        return EntityAttributeDTO.builder()
                .id(a.getId())
                .name(a.getName())
                .attrType(a.getAttrType())
                .dataType(a.getDataType())
                .relationTarget(a.getRelationTarget())
                .enumValuesJson(enumJson)

                .listVisible(a.getListVisible())
                .listOrder(a.getListOrder())
                .listLabel(a.getListLabel())
                .listWidthPx(a.getListWidthPx())
                .listAlign(a.getListAlign())
                .listSearchable(a.getListSearchable())
                .listSortable(a.getListSortable())

                .formVisible(a.getFormVisible())
                .formOrder(a.getFormOrder())
                .formLabel(a.getFormLabel())
                .controlType(a.getControlType())
                .placeholder(a.getPlaceholder())
                .helpText(a.getHelpText())
                .required(a.getRequired())
                .readOnly(a.getReadOnly())

                .minNum(a.getMinNum())
                .maxNum(a.getMaxNum())
                .minLen(a.getMinLen())
                .maxLen(a.getMaxLen())
                .pattern(a.getPattern())

                .defaultValue(a.getDefaultValue())
                .optionsEndpoint(a.getOptionsEndpoint())
                .build();
    }

    // -------- DTO -> AppEntityAttribute (aplicar valores no nulos) --------
    public static void applyAttrDto(AppEntityAttribute a, EntityAttributeDTO d) {
        if (d == null)
            return;

        applyBasicFields(a, d);
        applyEnumValues(a, d);
        applyListFields(a, d);
        applyFormFields(a, d);
        applyValidationFields(a, d);
        applyOptionsFields(a, d);
    }

    private static void applyBasicFields(AppEntityAttribute a, EntityAttributeDTO d) {
        if (d.name() != null)
            a.setName(d.name());
        if (d.attrType() != null)
            a.setAttrType(d.attrType());
        if (d.dataType() != null)
            a.setDataType(d.dataType());
        if (d.relationTarget() != null)
            a.setRelationTarget(d.relationTarget());
    }

    private static void applyEnumValues(AppEntityAttribute a, EntityAttributeDTO d) {
        if (d.enumValuesJson() != null && !d.enumValuesJson().isBlank()) {
            try {
                List<String> parsed = MAPPER.readValue(d.enumValuesJson(), new TypeReference<List<String>>() {
                });
                a.setEnumValues(parsed);
            } catch (Exception ignore) {
            }
        }
    }

    private static void applyListFields(AppEntityAttribute a, EntityAttributeDTO d) {
        if (d.listVisible() != null)
            a.setListVisible(d.listVisible());
        if (d.listOrder() != null)
            a.setListOrder(d.listOrder());
        if (d.listLabel() != null)
            a.setListLabel(d.listLabel());
        if (d.listWidthPx() != null)
            a.setListWidthPx(d.listWidthPx());
        if (d.listAlign() != null)
            a.setListAlign(d.listAlign());
        if (d.listSearchable() != null)
            a.setListSearchable(d.listSearchable());
        if (d.listSortable() != null)
            a.setListSortable(d.listSortable());
    }

    private static void applyFormFields(AppEntityAttribute a, EntityAttributeDTO d) {
        if (d.formVisible() != null)
            a.setFormVisible(d.formVisible());
        if (d.formOrder() != null)
            a.setFormOrder(d.formOrder());
        if (d.formLabel() != null)
            a.setFormLabel(d.formLabel());
        if (d.controlType() != null)
            a.setControlType(d.controlType());
        if (d.placeholder() != null)
            a.setPlaceholder(d.placeholder());
        if (d.helpText() != null)
            a.setHelpText(d.helpText());
        if (d.required() != null)
            a.setRequired(d.required());
        if (d.readOnly() != null)
            a.setReadOnly(d.readOnly());
    }

    private static void applyValidationFields(AppEntityAttribute a, EntityAttributeDTO d) {
        if (d.minNum() != null)
            a.setMinNum(d.minNum());
        if (d.maxNum() != null)
            a.setMaxNum(d.maxNum());
        if (d.minLen() != null)
            a.setMinLen(d.minLen());
        if (d.maxLen() != null)
            a.setMaxLen(d.maxLen());
        if (d.pattern() != null)
            a.setPattern(d.pattern());
    }

    private static void applyOptionsFields(AppEntityAttribute a, EntityAttributeDTO d) {
        if (d.defaultValue() != null)
            a.setDefaultValue(d.defaultValue());
        if (d.optionsEndpoint() != null)
            a.setOptionsEndpoint(d.optionsEndpoint());
    }
}
