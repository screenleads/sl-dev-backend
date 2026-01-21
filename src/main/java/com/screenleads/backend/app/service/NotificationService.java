package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.NotificationChannel;
import com.screenleads.backend.app.domain.model.NotificationTemplate;
import com.screenleads.backend.app.web.dto.NotificationRequest;
import com.screenleads.backend.app.web.dto.NotificationResponse;

import java.util.List;
import java.util.Map;

/**
 * Service for sending notifications via different channels (email, SMS, push)
 */
public interface NotificationService {

    /**
     * Send a notification using a template
     *
     * @param templateId ID of the template to use
     * @param recipient  Email, phone, or device token depending on channel
     * @param variables  Variables to replace in template (e.g., {"couponCode": "ABC123"})
     * @return Response with delivery status
     */
    NotificationResponse sendFromTemplate(Long templateId, String recipient, Map<String, String> variables);

    /**
     * Send a direct notification without template
     *
     * @param request Notification details
     * @return Response with delivery status
     */
    NotificationResponse sendDirect(NotificationRequest request);

    /**
     * Send notifications to multiple recipients (bulk)
     *
     * @param templateId ID of the template
     * @param recipients List of recipients
     * @param variables  Common variables for all recipients
     * @return List of responses
     */
    List<NotificationResponse> sendBulk(Long templateId, List<String> recipients, Map<String, String> variables);

    /**
     * Schedule a notification for later delivery
     *
     * @param templateId  ID of the template
     * @param recipient   Recipient
     * @param variables   Variables
     * @param scheduledAt Unix timestamp for scheduled delivery
     * @return Response with scheduled status
     */
    NotificationResponse scheduleNotification(Long templateId, String recipient, Map<String, String> variables, Long scheduledAt);

    /**
     * Test template rendering without sending
     *
     * @param templateId ID of the template
     * @param variables  Variables to replace
     * @return Rendered content
     */
    String previewTemplate(Long templateId, Map<String, String> variables);

    /**
     * Check if a notification channel is configured and ready
     *
     * @param channel Channel to check (EMAIL, SMS, PUSH)
     * @return true if configured
     */
    boolean isChannelConfigured(NotificationChannel channel);
}
