package com.screenleads.backend.app.web.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AppEntityDTO {

    private Long id;

    /** Clave funcional: "company", "device", "promotion_lead", ... */
    private String resource;

    /** Nombre mostrado: "Company", "Device", ... */
    private String entityName;

    /** Nombre FQCN de la clase JPA (opcional). */
    private String className;

    /** Nombre de tabla física (opcional). */
    private String tableName;

    /** Tipo del ID: "Long", "UUID", etc. */
    private String idType;

    /** Endpoint base REST sin /api (p. ej., "/companies"). */
    private String endpointBase;

    /** Niveles de permiso (si aplican en tu política). */
    private Integer createLevel;
    private Integer readLevel;
    private Integer updateLevel;
    private Integer deleteLevel;

    /** Conteo de filas (null si no se pidió). */
    private Long rowCount;

    /** Atributos (nombre → tipo) en orden estable. */
    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();
}
