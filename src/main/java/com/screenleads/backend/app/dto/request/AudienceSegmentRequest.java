package com.screenleads.backend.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating or updating an audience segment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudienceSegmentRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotBlank(message = "Segment name is required")
    @Size(max = 100, message = "Segment name must not exceed 100 characters")
    private String name;

    private String description;

    /**
     * Filter rules as JSON object
     * Example: {
     *   "minPurchases": 5,
     *   "minSpent": 100.0,
     *   "lastVisitDays": 30,
     *   "minAge": 18,
     *   "maxAge": 65,
     *   "isActive": true
     * }
     */
    private Map<String, Object> filterRules;

    @Builder.Default
    private Boolean isActive = true;

    private String createdBy;

    private Map<String, Object> metadata;
}
