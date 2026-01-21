package com.screenleads.backend.app.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for notification operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    /**
     * Unique ID of the notification
     */
    private String notificationId;

    /**
     * Delivery status (SENT, FAILED, SCHEDULED, PENDING)
     */
    private String status;

    /**
     * Recipient address
     */
    private String recipient;

    /**
     * Success flag
     */
    private boolean success;

    /**
     * Error message if failed
     */
    private String errorMessage;

    /**
     * External provider response ID (SendGrid, Twilio, Firebase)
     */
    private String providerMessageId;

    /**
     * Timestamp of sending/scheduling
     */
    private Long timestamp;
}
