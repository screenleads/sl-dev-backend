package com.screenleads.backend.app.domain.model;

import java.time.Instant;
import java.util.Set;
import jakarta.persistence.*;
import lombok.*;

/**
 * Representa un dispositivo físico donde se muestran las promociones.
 * Incluye información de geolocalización, hardware y estado operativo.
 */
@Entity
@Table(name = "device", indexes = {
        @Index(name = "ix_device_uuid", columnList = "uuid", unique = true),
        @Index(name = "ix_device_company", columnList = "company_id"),
        @Index(name = "ix_device_type", columnList = "type_id"),
        @Index(name = "ix_device_status", columnList = "status"),
        @Index(name = "ix_device_online", columnList = "online"),
        @Index(name = "ix_device_location", columnList = "city, country")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Identificación básica ===
    @Column(nullable = false, unique = true, length = 64)
    private String uuid;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(name = "description_name", length = 255)
    private String descriptionName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_device_company"))
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_device_type"))
    private DeviceType type;

    @ManyToMany
    @JoinTable(name = "device_advice", joinColumns = @JoinColumn(name = "device_id"), inverseJoinColumns = @JoinColumn(name = "advice_id"))
    private Set<Advice> advices;

    // === Ubicación física ===
    @Column(name = "location_name", length = 255)
    private String locationName; // "Centro Comercial La Vaguada - Planta 2"

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "address_2", length = 255)
    private String address2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String region; // Comunidad Autónoma / Región

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 2)
    private String country; // ISO 3166-1 alpha-2 (ES, PT, FR, etc.)

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 50)
    private String timezone; // Europe/Madrid, America/New_York, etc.

    // === Hardware y configuración ===
    @Column(length = 100)
    private String model; // Marca/modelo del dispositivo

    @Column(name = "os_version", length = 50)
    private String osVersion; // Versión del sistema operativo

    @Column(name = "app_version", length = 20)
    private String appVersion; // Versión de la app ScreenLeads

    @Column(name = "storage_capacity_mb")
    private Integer storageCapacityMb;

    @Column(name = "battery_level")
    private Integer batteryLevel; // 0-100

    // === Estado operativo ===
    @Builder.Default
    private Boolean online = false;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.ACTIVE;

    // === Métricas agregadas ===
    @Column(name = "total_impressions")
    @Builder.Default
    private Long totalImpressions = 0L;

    @Column(name = "total_interactions")
    @Builder.Default
    private Long totalInteractions = 0L;

    @Column(name = "total_redemptions")
    @Builder.Default
    private Long totalRedemptions = 0L;

    // === Configuración ===
    @Builder.Default
    private Boolean touchEnabled = true;

    @Builder.Default
    private Boolean audioEnabled = true;

    @Column(name = "default_interval_seconds")
    @Builder.Default
    private Integer defaultIntervalSeconds = 30;

    // === Métodos helper ===

    /**
     * Marca el dispositivo como visto recientemente
     */
    public void updateLastSeen() {
        this.lastSeenAt = Instant.now();
        this.online = true;
    }

    /**
     * Actualiza el heartbeat del dispositivo
     */
    public void heartbeat() {
        this.lastHeartbeatAt = Instant.now();
        this.online = true;
    }

    /**
     * Incrementa el contador de impresiones
     */
    public void incrementImpressions() {
        this.totalImpressions++;
    }

    /**
     * Incrementa el contador de interacciones
     */
    public void incrementInteractions() {
        this.totalInteractions++;
    }

    /**
     * Incrementa el contador de canjes
     */
    public void incrementRedemptions() {
        this.totalRedemptions++;
    }

    /**
     * Verifica si el dispositivo está activo y online
     */
    public boolean isActiveAndOnline() {
        return status == DeviceStatus.ACTIVE && online;
    }

    /**
     * Obtiene la ubicación completa como string
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null)
            sb.append(address);
        if (address2 != null && !address2.isEmpty()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(address2);
        }
        if (city != null) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(city);
        }
        if (postalCode != null) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(postalCode);
        }
        if (country != null) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(country);
        }
        return sb.toString();
    }

    /**
     * Verifica si el dispositivo tiene geolocalización configurada
     */
    public boolean hasGeolocation() {
        return latitude != null && longitude != null;
    }
}