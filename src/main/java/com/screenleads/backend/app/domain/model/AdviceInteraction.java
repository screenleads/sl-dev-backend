package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing a user interaction with an Advice (promotion).
 * Tracks specific actions taken after viewing a promotion (clicks, shares, redemptions, etc).
 */
@Entity
@Table(name = "advice_interactions", indexes = {
    @Index(name = "idx_advice_interaction_impression", columnList = "impression_id"),
    @Index(name = "idx_advice_interaction_customer", columnList = "customer_id"),
    @Index(name = "idx_advice_interaction_type", columnList = "type"),
    @Index(name = "idx_advice_interaction_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdviceInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The impression that led to this interaction
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impression_id", nullable = false)
    private AdviceImpression impression;

    /**
     * Optional: Customer who performed the interaction (if identified)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /**
     * Timestamp when the interaction occurred
     */
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * Type of interaction (VIEW_DETAILS, REDEEM_START, SHARE, etc)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private InteractionType type;

    /**
     * Additional details about the interaction stored as JSON
     * Example: {"url": "https://...", "method": "email", "duration": 45}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    /**
     * Duration in seconds of the interaction (if applicable)
     * For example: video watch time, content reading time, etc
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * IP address from which the interaction originated
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * User agent string from the device
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Whether this interaction led to a successful outcome (conversion)
     * For example: completed purchase, signed up, redeemed, etc
     */
    @Column(name = "is_conversion")
    @Builder.Default
    private Boolean isConversion = false;
}
