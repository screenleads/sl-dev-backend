// src/main/java/com/screenleads/backend/app/domain/model/EntityPermission.java
package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "entity_permission", uniqueConstraints = @UniqueConstraint(name = "uk_entity_permission_resource", columnNames = "resource"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de recurso: "user", "company", "device", "media", "promotion",
     * "advice", ...
     */
    @Column(nullable = false, length = 80)
    private String resource;

    /** Nivel mínimo requerido por acción */
    @Column(name = "create_level", nullable = false)
    private Integer createLevel;

    @Column(name = "read_level", nullable = false)
    private Integer readLevel;

    @Column(name = "update_level", nullable = false)
    private Integer updateLevel;

    @Column(name = "delete_level", nullable = false)
    private Integer deleteLevel;
}
