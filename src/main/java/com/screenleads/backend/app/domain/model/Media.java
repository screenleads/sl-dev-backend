package com.screenleads.backend.app.domain.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "media",
indexes = {
@Index(name = "ix_media_company", columnList = "company_id"),
@Index(name = "ix_media_created_at", columnList = "created_at")
},
uniqueConstraints = @UniqueConstraint(name = "uk_media_src", columnNames = {"src"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(nullable = false, length = 255)
private String name;


@Column(nullable = false, length = 2048)
private String src;


@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "type_id", nullable = false,
foreignKey = @ForeignKey(name = "fk_media_type"))
private MediaType type;


@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "company_id", nullable = false,
foreignKey = @ForeignKey(name = "fk_media_company"))
private Company company;
}