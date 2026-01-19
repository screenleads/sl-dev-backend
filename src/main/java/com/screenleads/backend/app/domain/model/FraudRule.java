package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Regla de detección de fraude
 */
@Entity
@Table(name = "fraud_rule", indexes = {
        @Index(name = "ix_frule_company", columnList = "company_id"),
        @Index(name = "ix_frule_type", columnList = "rule_type"),
        @Index(name = "ix_frule_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_frule_company"))
    private Company company;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 30)
    private FraudRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FraudSeverity severity = FraudSeverity.MEDIUM;

    /**
     * Configuración específica de la regla en JSONB
     * Ejemplo para VELOCITY: {"maxRedemptions": 10, "timeWindowMinutes": 60}
     * Ejemplo para LOCATION_ANOMALY: {"maxDistanceKm": 100, "timeWindowMinutes": 5}
     */
    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> configuration;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Si está activo, se generan alertas automáticas
     */
    @Column(name = "auto_alert", nullable = false)
    @Builder.Default
    private Boolean autoAlert = true;

    /**
     * Si está activo, bloquea automáticamente la acción
     */
    @Column(name = "auto_block", nullable = false)
    @Builder.Default
    private Boolean autoBlock = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_frule_creator"))
    private User createdBy;
}
