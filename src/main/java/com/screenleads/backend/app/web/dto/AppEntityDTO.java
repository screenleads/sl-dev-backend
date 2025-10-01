package com.screenleads.backend.app.web.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder(toBuilder = true)
public record AppEntityDTO(
        Long id,
        String resource,
        String entityName,
        String className,
        String tableName,
        String idType,
        String endpointBase,
        Integer createLevel,
        Integer readLevel,
        Integer updateLevel,
        Integer deleteLevel,
        Boolean visibleInMenu,
        Long rowCount,
        String displayLabel,
        String icon,
        Integer sortOrder,
        List<EntityAttributeDTO> attributes) {
}
