package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.NotificationChannel;
import com.screenleads.backend.app.domain.model.NotificationTemplate;
import com.screenleads.backend.app.domain.repository.NotificationTemplateRepository;
import com.screenleads.backend.app.service.NotificationService;
import com.screenleads.backend.app.web.dto.NotificationRequest;
import com.screenleads.backend.app.web.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic implementation of NotificationService
 * TODO: Integrate with SendGrid (email), Twilio (SMS), Firebase (push)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationTemplateRepository templateRepository;

    // Regex pattern for template variables: {{variableName}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    @Override
    public NotificationResponse sendFromTemplate(Long templateId, String recipient, Map<String, String> variables) {
        log.info("📧 Sending notification from template {} to {}", templateId, recipient);

        try {
            // Get template
            NotificationTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

            // Render template
            String subject = replaceVariables(template.getSubject(), variables);
            String body = replaceVariables(template.getBody(), variables);

            // Create direct notification request
            NotificationRequest request = NotificationRequest.builder()
                    .channel(template.getChannel())
                    .recipient(recipient)
                    .subject(subject)
                    .body(body)
                    .htmlBody(template.getHtmlBody())
                    .senderName(template.getSender())
                    .senderAddress(template.getReplyTo())
                    .build();

            return sendDirect(request);

        } catch (Exception e) {
            log.error("❌ Error sending notification from template {}: {}", templateId, e.getMessage());
            return NotificationResponse.builder()
                    .notificationId(UUID.randomUUID().toString())
                    .status("FAILED")
                    .recipient(recipient)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    @Override
    public NotificationResponse sendDirect(NotificationRequest request) {
        log.info("📤 Sending direct notification via {} to {}", request.getChannel(), request.getRecipient());

        String notificationId = UUID.randomUUID().toString();

        try {
            // Route to appropriate channel
            NotificationResponse response;
            switch (request.getChannel()) {
                case EMAIL:
                    response = sendEmail(notificationId, request);
                    break;
                case SMS:
                    response = sendSMS(notificationId, request);
                    break;
                case PUSH_NOTIFICATION:
                    response = sendPush(notificationId, request);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported channel: " + request.getChannel());
            }
            return response;

        } catch (Exception e) {
            log.error("❌ Error sending direct notification: {}", e.getMessage());
            return NotificationResponse.builder()
                    .notificationId(notificationId)
                    .status("FAILED")
                    .recipient(request.getRecipient())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    @Override
    public List<NotificationResponse> sendBulk(Long templateId, List<String> recipients, Map<String, String> variables) {
        log.info("📬 Sending bulk notifications from template {} to {} recipients", templateId, recipients.size());

        List<NotificationResponse> responses = new ArrayList<>();

        for (String recipient : recipients) {
            NotificationResponse response = sendFromTemplate(templateId, recipient, variables);
            responses.add(response);
        }

        long successCount = responses.stream().filter(NotificationResponse::isSuccess).count();
        log.info("✅ Bulk send complete: {}/{} successful", successCount, recipients.size());

        return responses;
    }

    @Override
    public NotificationResponse scheduleNotification(Long templateId, String recipient, Map<String, String> variables, Long scheduledAt) {
        log.info("⏰ Scheduling notification from template {} for recipient {} at {}", templateId, recipient, scheduledAt);

        // TODO: Implement actual scheduling (e.g., using Spring @Scheduled or job queue)
        // For now, return a SCHEDULED status
        return NotificationResponse.builder()
                .notificationId(UUID.randomUUID().toString())
                .status("SCHEDULED")
                .recipient(recipient)
                .success(true)
                .timestamp(scheduledAt)
                .build();
    }

    @Override
    public String previewTemplate(Long templateId, Map<String, String> variables) {
        log.info("👁️ Previewing template {}", templateId);

        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        String subject = replaceVariables(template.getSubject(), variables);
        String body = replaceVariables(template.getBody(), variables);

        return String.format("Subject: %s\n\nBody:\n%s", subject, body);
    }

    @Override
    public boolean isChannelConfigured(NotificationChannel channel) {
        // TODO: Check if API keys/credentials are configured
        switch (channel) {
            case EMAIL:
                // Check if SendGrid API key is set
                return false; // TODO: return sendGridApiKey != null && !sendGridApiKey.isEmpty();
            case SMS:
                // Check if Twilio credentials are set
                return false; // TODO: return twilioAccountSid != null && twilioAuthToken != null;
            case PUSH_NOTIFICATION:
                // Check if Firebase credentials are set
                return false; // TODO: return firebaseProjectId != null;
            default:
                return false;
        }
    }

    /**
     * Send email notification (basic implementation)
     * TODO: Integrate with SendGrid
     */
    private NotificationResponse sendEmail(String notificationId, NotificationRequest request) {
        log.info("📧 Sending email to {}: {}", request.getRecipient(), request.getSubject());

        // TODO: Integrate with SendGrid API
        // For now, just log and return success
        log.warn("⚠️ Email sending not yet implemented - would send to: {}", request.getRecipient());

        return NotificationResponse.builder()
                .notificationId(notificationId)
                .status("SENT")
                .recipient(request.getRecipient())
                .success(true)
                .providerMessageId("mock-email-" + notificationId)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Send SMS notification (basic implementation)
     * TODO: Integrate with Twilio
     */
    private NotificationResponse sendSMS(String notificationId, NotificationRequest request) {
        log.info("📱 Sending SMS to {}", request.getRecipient());

        // TODO: Integrate with Twilio API
        log.warn("⚠️ SMS sending not yet implemented - would send to: {}", request.getRecipient());

        return NotificationResponse.builder()
                .notificationId(notificationId)
                .status("SENT")
                .recipient(request.getRecipient())
                .success(true)
                .providerMessageId("mock-sms-" + notificationId)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Send push notification (basic implementation)
     * TODO: Integrate with Firebase Cloud Messaging
     */
    private NotificationResponse sendPush(String notificationId, NotificationRequest request) {
        log.info("🔔 Sending push notification to device {}", request.getRecipient());

        // TODO: Integrate with Firebase Cloud Messaging
        log.warn("⚠️ Push notification not yet implemented - would send to: {}", request.getRecipient());

        return NotificationResponse.builder()
                .notificationId(notificationId)
                .status("SENT")
                .recipient(request.getRecipient())
                .success(true)
                .providerMessageId("mock-push-" + notificationId)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Replace template variables with actual values
     * Example: "Hello {{name}}, your code is {{code}}" with {name: "John", code: "123"}
     * becomes "Hello John, your code is 123"
     */
    private String replaceVariables(String template, Map<String, String> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = variables.getOrDefault(variableName, "{{" + variableName + "}}");
            result = result.replace("{{" + variableName + "}}", value);
        }

        return result;
    }
}
