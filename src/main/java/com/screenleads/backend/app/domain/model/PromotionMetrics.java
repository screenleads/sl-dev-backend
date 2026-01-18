package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity for storing aggregated daily metrics for each Advice/Promotion.
 * This data is calculated by a scheduled job that runs nightly.
 */
@Entity
@Table(name = "promotion_metrics", indexes = {
    @Index(name = "idx_promotion_metrics_date", columnList = "metric_date"),
    @Index(name = "idx_promotion_metrics_advice", columnList = "advice_id"),
    @Index(name = "idx_promotion_metrics_date_advice", columnList = "metric_date, advice_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Date for which these metrics were calculated (daily aggregation)
     */
    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    /**
     * The Advice/Promotion for which metrics are calculated
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advice_id", nullable = false)
    private Advice advice;

    /**
     * Total number of impressions for this day
     */
    @Column(name = "total_impressions", nullable = false)
    @Builder.Default
    private Long totalImpressions = 0L;

    /**
     * Total number of interactions for this day
     */
    @Column(name = "total_interactions", nullable = false)
    @Builder.Default
    private Long totalInteractions = 0L;

    /**
     * Total number of conversions (successful redemptions, purchases, etc.)
     */
    @Column(name = "total_conversions", nullable = false)
    @Builder.Default
    private Long totalConversions = 0L;

    /**
     * Number of unique customers who viewed this promotion
     */
    @Column(name = "unique_customers", nullable = false)
    @Builder.Default
    private Long uniqueCustomers = 0L;

    /**
     * Number of unique devices that showed this promotion
     */
    @Column(name = "unique_devices", nullable = false)
    @Builder.Default
    private Long uniqueDevices = 0L;

    /**
     * Conversion rate: (conversions / impressions) * 100
     */
    @Column(name = "conversion_rate", precision = 5, scale = 2)
    private BigDecimal conversionRate;

    /**
     * Click-through rate: (interactions / impressions) * 100
     */
    @Column(name = "click_through_rate", precision = 5, scale = 2)
    private BigDecimal clickThroughRate;

    /**
     * Average duration in seconds users spent viewing this promotion
     */
    @Column(name = "avg_view_duration_seconds")
    private Double avgViewDurationSeconds;

    /**
     * Total revenue generated (if applicable, from redemptions)
     */
    @Column(name = "total_revenue", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    /**
     * Timestamp when these metrics were calculated
     */
    @CreationTimestamp
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;

    /**
     * Additional metadata about the calculation (JSON)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Calculate conversion rate based on impressions and conversions
     */
    public void calculateConversionRate() {
        if (totalImpressions > 0) {
            double rate = (totalConversions.doubleValue() / totalImpressions.doubleValue()) * 100;
            this.conversionRate = BigDecimal.valueOf(Math.round(rate * 100.0) / 100.0);
        } else {
            this.conversionRate = BigDecimal.ZERO;
        }
    }

    /**
     * Calculate click-through rate based on impressions and interactions
     */
    public void calculateClickThroughRate() {
        if (totalImpressions > 0) {
            double rate = (totalInteractions.doubleValue() / totalImpressions.doubleValue()) * 100;
            this.clickThroughRate = BigDecimal.valueOf(Math.round(rate * 100.0) / 100.0);
        } else {
            this.clickThroughRate = BigDecimal.ZERO;
        }
    }
}
