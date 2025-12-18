package com.screenleads.backend.app.domain.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "app_version",
uniqueConstraints = @UniqueConstraint(
name = "uk_appversion_platform_version",
columnNames = {"platform", "version"}
),
indexes = @Index(name = "ix_appversion_platform", columnList = "platform")
)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersion extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(nullable = false, length = 20)
private String platform; // "android" o "ios"


@Column(nullable = false, length = 40)
private String version;


@Column(length = 255)
private String message;


@Column(nullable = false, length = 2048)
private String url;


@Column(name = "force_update", nullable = false)
private boolean forceUpdate;
}