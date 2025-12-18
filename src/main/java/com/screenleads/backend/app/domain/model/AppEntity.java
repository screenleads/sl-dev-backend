package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_entity", uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_entity_resource", columnNames = "resource"),
                @UniqueConstraint(name = "uk_app_entity_endpoint", columnNames = "endpoint_base")
}, indexes = {
                @Index(name = "ix_app_entity_endpoint", columnList = "endpoint_base"),
                @Index(name = "ix_app_entity_sort_order", columnList = "sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /** Clave funcional: "company", "device", "promotion_lead", ... */
        @Column(nullable = false, length = 80)
        private String resource;

        /** Nombre "técnico" mostrable (normalmente igual al nombre de clase simple). */
        @Column(name = "entity_name", nullable = false, length = 120)
        private String entityName;

        /** Nombre FQCN (opcional). */
        @Column(name = "class_name", length = 300)
        private String className;

        /** Nombre de tabla física (opcional). */
        @Column(name = "table_name", length = 120)
        private String tableName;

        /** Tipo del ID: "Long", "UUID", etc. */
        @Column(name = "id_type", length = 60)
        private String idType;

        /** Endpoint base REST sin /api (p. ej., "/users"). */
        @Column(name = "endpoint_base", nullable = false, length = 120)
        private String endpointBase;

        /** Niveles de permiso (si aplican en tu política). */
        @Column(name = "create_level", nullable = false)
        private Integer createLevel;

        @Column(name = "read_level", nullable = false)
        private Integer readLevel;

        @Column(name = "update_level", nullable = false)
        private Integer updateLevel;

        @Column(name = "delete_level", nullable = false)
        private Integer deleteLevel;

        @Column(name = "visible_in_menu")
        private Boolean visibleInMenu;

        /** Conteo de filas (opcional; null si no se pide). */
        @Column(name = "row_count")
        private Long rowCount;

        /** Texto mostrado en el dashboard/menú (p.ej., "Devices"). */
        @Column(name = "display_label", nullable = false, length = 120)
        private String displayLabel;

        /** Nombre de icono (p.ej., "tv-2", "building-2", "image"). */
        @Column(name = "icon", length = 80)
        private String icon;

        /** Orden en el menú (más bajo = antes). */
        @Column(name = "sort_order")
        private Integer sortOrder;

        // AppEntity.java (solo la parte relevante)
        @OneToMany(mappedBy = "appEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        @OrderBy("listOrder ASC NULLS LAST, id ASC")
        @Builder.Default
        private java.util.List<AppEntityAttribute> attributes = new java.util.ArrayList<>();

}
