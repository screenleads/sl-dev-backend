package com.screenleads.backend.app.web.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
public record EntityAttributeDTO(
        // Identidad + tipado
        Long id,
        String name,
        String attrType, // e.g. "BASIC", "RELATION", "ENUM"
        String dataType, // e.g. "String", "Long", "Boolean", "Duration", etc.
        String relationTarget, // si RELATION: nombre entidad destino
        String enumValuesJson, // si ENUM: JSON con valores

        // List / tabla
        Boolean listVisible,
        Integer listOrder,
        String listLabel,
        Integer listWidthPx,
        String listAlign,
        Boolean listSearchable,
        Boolean listSortable,

        // Form
        Boolean formVisible,
        Integer formOrder,
        String formLabel,
        String controlType, // input, select, textarea, color, date, time, etc.
        String placeholder,
        String helpText,
        Boolean required,
        Boolean readOnly,

        // Validaciones
        BigDecimal minNum,
        BigDecimal maxNum,
        Integer minLen,
        Integer maxLen,
        String pattern,

        // Otros
        String defaultValue,
        String optionsEndpoint) {
}
