package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Evento de geofencing (entrada/salida/permanencia en zona)
 */
@Entity
@Table(name = "geofence_event",
    indexes = {
        @Index(name = "ix_gfevent_device", columnList = "device_id"),
        @Index(name = "ix_gfevent_zone", columnList = "zone_id"),
        @Index(name = "ix_gfevent_timestamp", columnList = "timestamp"),
        @Index(name = "ix_gfevent_type", columnList = "event_type")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeofenceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_gfevent_device"))
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_gfevent_zone"))
    private GeofenceZone zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private GeofenceEventType eventType;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    /**
     * Precisión de la ubicación en metros
     */
    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    /**
     * Tiempo de permanencia en la zona (para eventos DWELL)
     */
    @Column(name = "dwell_time_seconds")
    private Integer dwellTimeSeconds;

    /**
     * Metadata adicional del evento
     */
    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
