package com.screenleads.backend.app.domain.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "device_type",
uniqueConstraints = @UniqueConstraint(name = "uk_devicetype_type", columnNames = "type")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceType {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(nullable = false, length = 50)
private String type;


@Column(nullable = false)
@Builder.Default
private Boolean enabled = Boolean.TRUE;
}