package com.screenleads.backend.app.web.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AppEntityDTO {

    private Long id;

    private String resource;
    private String entityName;
    private String className;
    private String tableName;
    private String idType;
    private String endpointBase;

    private Integer createLevel;
    private Integer readLevel;
    private Integer updateLevel;
    private Integer deleteLevel;

    private Long rowCount;

    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();

    // Dashboard metadata
    private String displayLabel;
    private String icon;
    private Integer sortOrder;
}
