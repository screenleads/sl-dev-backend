package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Entity for notification templates used in marketing campaigns
 */
@Entity
@Table(name = "notification_templates", indexes = {
        @Index(name = "idx_notification_template_company", columnList = "company_id"),
        @Index(name = "idx_notification_template_channel", columnList = "channel")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Company that owns this template
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * Name of the template for identification
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Description of the template's purpose
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Channel for this notification (EMAIL, SMS, PUSH, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel;

    /**
     * Subject line for email notifications (or title for push notifications)
     * Supports variables: {{customerName}}, {{promotionTitle}}, etc.
     */
    @Column(name = "subject", length = 200)
    private String subject;

    /**
     * Main body content of the notification
     * Supports variables: {{customerName}}, {{promotionTitle}}, {{discountAmount}},
     * etc.
     */
    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    /**
     * List of available variables that can be used in this template
     * Example: ["customerName", "promotionTitle", "discountAmount", "expiryDate"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "available_variables", columnDefinition = "jsonb")
    private List<String> availableVariables;

    /**
     * Optional HTML version of the body (for email)
     */
    @Column(name = "html_body", columnDefinition = "TEXT")
    private String htmlBody;

    /**
     * Whether this template is currently active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Sender information (email address, phone number, etc.)
     */
    @Column(name = "sender", length = 100)
    private String sender;

    /**
     * Reply-to address for emails
     */
    @Column(name = "reply_to", length = 100)
    private String replyTo;

    /**
     * Additional metadata for the template (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Timestamp when this template was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this template was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User who created this template
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Number of times this template has been used
     */
    @Column(name = "usage_count")
    @Builder.Default
    private Long usageCount = 0L;

    /**
     * Last time this template was used
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
