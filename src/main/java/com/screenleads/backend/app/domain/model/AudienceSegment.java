package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for audience segmentation - defines groups of customers based on criteria
 * Used for targeted marketing campaigns and personalized promotions
 */
@Entity
@Table(name = "audience_segments", indexes = {
    @Index(name = "idx_audience_segment_company", columnList = "company_id"),
    @Index(name = "idx_audience_segment_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudienceSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Company that owns this audience segment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * Name of the audience segment (e.g., "Frequent Shoppers", "New Customers")
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Description of the segment and its purpose
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * JSON object containing filter rules for segment membership
     * Example: {
     *   "minPurchases": 5,
     *   "minSpent": 100.00,
     *   "lastVisitDays": 30,
     *   "favoriteCategories": ["Electronics", "Fashion"],
     *   "locations": ["New York", "Los Angeles"]
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filter_rules", columnDefinition = "jsonb")
    private Map<String, Object> filterRules;

    /**
     * Whether this segment is currently active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Cached count of customers matching this segment (updated periodically)
     */
    @Column(name = "customer_count")
    @Builder.Default
    private Long customerCount = 0L;

    /**
     * Last time the customer count was calculated
     */
    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    /**
     * Timestamp when this segment was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this segment was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User who created this segment
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Additional metadata for the segment (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Check if customer count needs recalculation (older than 1 hour)
     */
    public boolean needsRecalculation() {
        if (lastCalculatedAt == null) {
            return true;
        }
        return lastCalculatedAt.isBefore(LocalDateTime.now().minusHours(1));
    }
}
