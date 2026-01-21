package com.screenleads.backend.app.web.dto;

import com.screenleads.backend.app.domain.model.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending direct notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    /**
     * Channel to use for notification
     */
    private NotificationChannel channel;

    /**
     * Recipient (email, phone number, or device token)
     */
    private String recipient;

    /**
     * Subject/title of the notification
     */
    private String subject;

    /**
     * Body/content of the notification
     */
    private String body;

    /**
     * Optional HTML content (for email)
     */
    private String htmlBody;

    /**
     * Sender name (optional)
     */
    private String senderName;

    /**
     * Sender email/phone (optional)
     */
    private String senderAddress;
}
