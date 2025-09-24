// src/main/java/com/screenleads/backend/app/domain/model/Role.java
package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role", indexes = @Index(name = "ix_role_role", columnList = "role", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Nombre técnico, p.ej. "ROLE_ADMIN" */
  @Column(nullable = false, unique = true, length = 60)
  private String role;

  /** Descripción legible */
  @Column(length = 255)
  private String description;

  /** Nivel de poder: 1 = máximo privilegio, 2 = menos, ... */
  @Column(nullable = false)
  private Integer level;
}
