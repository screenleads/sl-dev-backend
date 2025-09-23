package com.screenleads.backend.app.domain.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "media_type",
uniqueConstraints = @UniqueConstraint(name = "uk_mediatype_type", columnNames = "type")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaType {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Builder.Default
@Column(name="enabled", nullable=false)
private Boolean enabled = Boolean.TRUE;

@Column(nullable = false, length = 50)
private String type; // e.g., IMAGE, VIDEO


@Column(nullable = false, length = 10)
private String extension; // e.g., jpg, mp4
}