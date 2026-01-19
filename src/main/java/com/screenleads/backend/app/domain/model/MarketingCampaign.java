package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Campaña de marketing dirigida a un segmento de audiencia específico
 * utilizando plantillas de notificación personalizadas
 */
@Entity
@Table(name = "marketing_campaign", indexes = {
        @Index(name = "ix_campaign_company", columnList = "company_id"),
        @Index(name = "ix_campaign_status", columnList = "status"),
        @Index(name = "ix_campaign_scheduled", columnList = "scheduled_at"),
        @Index(name = "ix_campaign_segment", columnList = "audience_segment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketingCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_campaign_company"))
    private Company company;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    // === Configuración de la campaña ===

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audience_segment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_campaign_segment"))
    private AudienceSegment audienceSegment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_template_id", nullable = false, foreignKey = @ForeignKey(name = "fk_campaign_template"))
    private NotificationTemplate notificationTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    // === Programación ===

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // === Métricas de ejecución ===

    @Column(name = "target_audience_size")
    @Builder.Default
    private Long targetAudienceSize = 0L;

    @Column(name = "sent_count")
    @Builder.Default
    private Long sentCount = 0L;

    @Column(name = "success_count")
    @Builder.Default
    private Long successCount = 0L;

    @Column(name = "failed_count")
    @Builder.Default
    private Long failedCount = 0L;

    @Column(name = "open_count")
    @Builder.Default
    private Long openCount = 0L;

    @Column(name = "click_count")
    @Builder.Default
    private Long clickCount = 0L;

    // === Metadata adicional (JSONB) ===

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> metadata;

    // === Resultado de la ejecución ===

    @Column(name = "execution_error", length = 2000)
    private String executionError;

    // === Auditoría ===

    @Column(name = "created_at", nullable = false, updatable = false)
    @org.hibernate.annotations.CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @org.hibernate.annotations.UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_campaign_created_by"))
    private User createdBy;

    // === Métodos de utilidad ===

    /**
     * Verifica si la campaña puede ser ejecutada
     */
    public boolean canBeExecuted() {
        return status == CampaignStatus.SCHEDULED &&
                scheduledAt != null &&
                scheduledAt.isBefore(LocalDateTime.now());
    }

    /**
     * Verifica si la campaña está activa (en ejecución)
     */
    public boolean isActive() {
        return status == CampaignStatus.RUNNING;
    }

    /**
     * Verifica si la campaña está completada o cancelada
     */
    public boolean isFinalized() {
        return status == CampaignStatus.COMPLETED ||
                status == CampaignStatus.CANCELLED ||
                status == CampaignStatus.FAILED;
    }

    /**
     * Calcula la tasa de éxito de envío
     */
    public double getSuccessRate() {
        if (sentCount == null || sentCount == 0) {
            return 0.0;
        }
        return (successCount != null ? successCount : 0) * 100.0 / sentCount;
    }

    /**
     * Calcula la tasa de apertura
     */
    public double getOpenRate() {
        if (sentCount == null || sentCount == 0) {
            return 0.0;
        }
        return (openCount != null ? openCount : 0) * 100.0 / sentCount;
    }

    /**
     * Calcula la tasa de clicks
     */
    public double getClickRate() {
        if (sentCount == null || sentCount == 0) {
            return 0.0;
        }
        return (clickCount != null ? clickCount : 0) * 100.0 / sentCount;
    }
}
