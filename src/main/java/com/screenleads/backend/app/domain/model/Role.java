package com.screenleads.backend.app.domain.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "role",
uniqueConstraints = @UniqueConstraint(name = "uk_role_name", columnNames = "role"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(name = "role", nullable = false, length = 50)
private String role; // e.g., ROLE_ADMIN, ROLE_USER
}