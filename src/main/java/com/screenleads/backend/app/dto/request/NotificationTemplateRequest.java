package com.screenleads.backend.app.dto.request;

import com.screenleads.backend.app.domain.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating or updating a notification template
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotBlank(message = "Template name is required")
    @Size(max = 100, message = "Template name must not exceed 100 characters")
    private String name;

    private String description;

    @NotNull(message = "Notification channel is required")
    private NotificationChannel channel;

    private String subject;

    @NotBlank(message = "Body is required")
    private String body;

    private String htmlBody;

    private List<String> availableVariables;

    @Builder.Default
    private Boolean isActive = true;

    private String sender;

    private String replyTo;

    private String createdBy;

    private Map<String, Object> metadata;
}
