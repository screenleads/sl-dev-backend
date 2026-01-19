package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Zona geográfica para geofencing de promociones
 */
@Entity
@Table(name = "geofence_zone", indexes = {
        @Index(name = "ix_geofence_company", columnList = "company_id"),
        @Index(name = "ix_geofence_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeofenceZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_geofence_company"))
    private Company company;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GeofenceType type;

    /**
     * Geometría en formato JSON
     * CIRCLE: {"center": {"lat": 40.4168, "lon": -3.7038}, "radius": 1000}
     * POLYGON: {"coordinates": [{"lat": 40.4, "lon": -3.7}, {"lat": 40.5, "lon":
     * -3.6}, ...]}
     * RECTANGLE: {"sw": {"lat": 40.4, "lon": -3.7}, "ne": {"lat": 40.5, "lon":
     * -3.6}}
     */
    @Column(columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> geometry;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Color hex para visualización en mapa (#FF0000)
     */
    @Column(length = 7)
    @Builder.Default
    private String color = "#3B82F6";

    /**
     * Metadata adicional para la zona
     */
    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_geofence_created_by"))
    private User createdBy;

    /**
     * Verifica si un punto está dentro de la zona
     */
    public boolean containsPoint(double latitude, double longitude) {
        if (geometry == null || type == null) {
            return false;
        }

        return switch (type) {
            case CIRCLE -> containsPointInCircle(latitude, longitude);
            case POLYGON -> containsPointInPolygon(latitude, longitude);
            case RECTANGLE -> containsPointInRectangle(latitude, longitude);
        };
    }

    private boolean containsPointInCircle(double lat, double lon) {
        @SuppressWarnings("unchecked")
        Map<String, Double> center = (Map<String, Double>) geometry.get("center");
        Number radiusNum = (Number) geometry.get("radius");

        if (center == null || radiusNum == null) {
            return false;
        }

        double centerLat = center.get("lat");
        double centerLon = center.get("lon");
        double radius = radiusNum.doubleValue();

        double distance = calculateDistance(lat, lon, centerLat, centerLon);
        return distance <= radius;
    }

    private boolean containsPointInPolygon(double lat, double lon) {
        // Implementación simplificada - Ray casting algorithm
        // En producción usar PostGIS ST_Contains
        return false; // TODO: Implementar algoritmo completo
    }

    private boolean containsPointInRectangle(double lat, double lon) {
        @SuppressWarnings("unchecked")
        Map<String, Double> sw = (Map<String, Double>) geometry.get("sw");
        @SuppressWarnings("unchecked")
        Map<String, Double> ne = (Map<String, Double>) geometry.get("ne");

        if (sw == null || ne == null) {
            return false;
        }

        double swLat = sw.get("lat");
        double swLon = sw.get("lon");
        double neLat = ne.get("lat");
        double neLon = ne.get("lon");

        return lat >= swLat && lat <= neLat && lon >= swLon && lon <= neLon;
    }

    /**
     * Calcula distancia en metros usando fórmula Haversine
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radio de la Tierra en metros

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
