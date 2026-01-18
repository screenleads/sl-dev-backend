package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Lista negra de IPs, dispositivos, emails, etc.
 */
@Entity
@Table(name = "blacklist",
    indexes = {
        @Index(name = "ix_blacklist_type", columnList = "blacklist_type"),
        @Index(name = "ix_blacklist_value", columnList = "value"),
        @Index(name = "ix_blacklist_company", columnList = "company_id"),
        @Index(name = "ix_blacklist_active", columnList = "is_active")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_blacklist_type_value_company",
            columnNames = {"blacklist_type", "value", "company_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_blacklist_company"))
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "blacklist_type", nullable = false, length = 20)
    private BlacklistType blacklistType;

    @Column(nullable = false, length = 255)
    private String value;

    @Column(length = 500)
    private String reason;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Fecha de expiración del bloqueo (null = permanente)
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by",
        foreignKey = @ForeignKey(name = "fk_blacklist_creator"))
    private User createdBy;

    /**
     * Referencia a la alerta que generó este bloqueo (si aplica)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id",
        foreignKey = @ForeignKey(name = "fk_blacklist_alert"))
    private FraudAlert alert;

    /**
     * Verifica si el bloqueo está activo y no ha expirado
     */
    public boolean isEffective() {
        if (!isActive) {
            return false;
        }
        if (expiresAt == null) {
            return true;
        }
        return LocalDateTime.now().isBefore(expiresAt);
    }
}
