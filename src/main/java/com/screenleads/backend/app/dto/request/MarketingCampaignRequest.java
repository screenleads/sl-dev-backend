package com.screenleads.backend.app.dto.request;

import com.screenleads.backend.app.domain.model.CampaignStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketingCampaignRequest {

    @NotBlank(message = "Campaign name is required")
    @Size(max = 150, message = "Name cannot exceed 150 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Audience segment ID is required")
    private Long audienceSegmentId;

    @NotNull(message = "Notification template ID is required")
    private Long notificationTemplateId;

    private LocalDateTime scheduledAt;

    private Map<String, Object> metadata;
}
