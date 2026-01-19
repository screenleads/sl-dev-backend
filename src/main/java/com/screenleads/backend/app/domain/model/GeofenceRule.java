package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Regla de geofencing aplicada a una promoción
 */
@Entity
@Table(name = "geofence_rule", indexes = {
        @Index(name = "ix_gfrule_promotion", columnList = "promotion_id"),
        @Index(name = "ix_gfrule_zone", columnList = "zone_id"),
        @Index(name = "ix_gfrule_priority", columnList = "priority")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_gfrule_promotion_zone", columnNames = { "promotion_id", "zone_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeofenceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_gfrule_promotion"))
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false, foreignKey = @ForeignKey(name = "fk_gfrule_zone"))
    private GeofenceZone zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private RuleType ruleType;

    /**
     * Prioridad de la regla (mayor número = mayor prioridad)
     * Usado cuando múltiples reglas aplican al mismo dispositivo
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Verifica si la regla permite mostrar la promoción en la ubicación dada
     */
    public boolean allowsPromotion(double latitude, double longitude) {
        if (!isActive || zone == null) {
            return true; // Si no está activa, no restringe
        }

        boolean isInside = zone.containsPoint(latitude, longitude);

        return switch (ruleType) {
            case SHOW_INSIDE -> isInside;
            case HIDE_OUTSIDE -> isInside;
            case PRIORITIZE_INSIDE -> true; // No bloquea, solo prioriza
            case BLOCK_OUTSIDE -> isInside;
        };
    }
}
