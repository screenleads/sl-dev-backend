package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an impression/view of an Advice (promotion) on a Device.
 * Used for analytics to track promotion visibility and engagement.
 */
@Entity
@Table(name = "advice_impressions", indexes = {
        @Index(name = "idx_advice_impression_advice", columnList = "advice_id"),
        @Index(name = "idx_advice_impression_device", columnList = "device_id"),
        @Index(name = "idx_advice_impression_customer", columnList = "customer_id"),
        @Index(name = "idx_advice_impression_timestamp", columnList = "timestamp"),
        @Index(name = "idx_advice_impression_session", columnList = "session_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdviceImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The Advice (promotion) that was displayed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advice_id", nullable = false)
    private Advice advice;

    /**
     * The Device where the Advice was displayed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /**
     * Optional: Customer who viewed the promotion (if identified)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /**
     * Timestamp when the impression occurred
     */
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * Duration in seconds that the Advice was visible (0 if not tracked)
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Session identifier to group multiple impressions from the same customer
     * session
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * IP address of the device (for fraud detection)
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * Whether the customer interacted with the promotion (clicked, viewed details,
     * etc)
     */
    @Column(name = "was_interactive")
    @Builder.Default
    private Boolean wasInteractive = false;

    /**
     * User agent string from the device (for analytics)
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Geographic location where the impression occurred (latitude)
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * Geographic location where the impression occurred (longitude)
     */
    @Column(name = "longitude")
    private Double longitude;
}
