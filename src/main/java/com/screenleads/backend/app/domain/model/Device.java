package com.screenleads.backend.app.domain.model;


import java.util.Set;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "device",
indexes = {
@Index(name = "ix_device_uuid", columnList = "uuid", unique = true),
@Index(name = "ix_device_company", columnList = "company_id"),
@Index(name = "ix_device_type", columnList = "type_id")
}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(nullable = false, unique = true, length = 64)
private String uuid;




@Column(nullable = false)
private Integer width;


@Column(nullable = false)
private Integer height;


@Column(name="description_name", length=255)
private String descriptionName;


@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "company_id", nullable = false,
foreignKey = @ForeignKey(name = "fk_device_company"))
private Company company;


@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "type_id", nullable = false,
foreignKey = @ForeignKey(name = "fk_device_type"))
private DeviceType type;


@ManyToMany
@JoinTable(name = "device_advice",
joinColumns = @JoinColumn(name = "device_id"),
inverseJoinColumns = @JoinColumn(name = "advice_id"))
private Set<Advice> advices;
}