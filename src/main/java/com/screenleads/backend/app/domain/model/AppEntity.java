// src/main/java/com/screenleads/backend/app/domain/model/AppEntity.java
package com.screenleads.backend.app.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "entity",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_entity_resource", columnNames = "resource"),
        @UniqueConstraint(name = "uk_entity_endpoint", columnNames = "endpoint_base")
    },
    indexes = {
        @Index(name = "ix_entity_endpoint", columnList = "endpoint_base")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AppEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Clave funcional: "company", "device", "promotion_lead", ... */
    @Column(nullable = false, length = 80)
    private String resource;

    /** Nombre mostrado en la tabla (p. ej. "Company"). */
    @Column(name = "entity_name", nullable = false, length = 120)
    private String entityName;

    /** Nombre FQCN de la clase JPA (si aplica). */
    @Column(name = "class_name", length = 300)
    private String className;

    /** Nombre de tabla física (si aplica). */
    @Column(name = "table_name", length = 120)
    private String tableName;

    /** Tipo de ID (p. ej. "Long"). */
    @Column(name = "id_type", length = 60)
    private String idType;

    /** Endpoint base REST sin /api (p. ej. "/users"). */
    @Column(name = "endpoint_base", nullable = false, length = 120)
    private String endpointBase;

    /** Niveles de permiso (si los usas). */
    @Column(name = "create_level", nullable = false)
    private Integer createLevel;

    @Column(name = "read_level", nullable = false)
    private Integer readLevel;

    @Column(name = "update_level", nullable = false)
    private Integer updateLevel;

    @Column(name = "delete_level", nullable = false)
    private Integer deleteLevel;

    /** Conteo de filas (opcional, puede quedar null) */
    @Column(name = "row_count")
    private Long rowCount;

    /** Atributos: nombre → tipo (guardados en tabla secundaria) */
    @ElementCollection
    @CollectionTable(
        name = "entity_attribute",
        joinColumns = @JoinColumn(name = "entity_id",
            foreignKey = @ForeignKey(name = "fk_entity_attribute_entity"))
    )
    @MapKeyColumn(name = "attr_name", length = 120)
    @Column(name = "attr_type", length = 200)
    @OrderColumn(name = "attr_order")
    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();
}
