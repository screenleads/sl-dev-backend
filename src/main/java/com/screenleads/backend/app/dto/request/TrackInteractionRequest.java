package com.screenleads.backend.app.dto.request;

import com.screenleads.backend.app.domain.model.InteractionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for tracking an Advice interaction
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackInteractionRequest {

    @NotNull(message = "Impression ID is required")
    private Long impressionId;

    /**
     * Optional: Customer ID if the user is identified
     */
    private Long customerId;

    @NotNull(message = "Interaction type is required")
    private InteractionType type;

    /**
     * Additional details about the interaction (JSON)
     */
    private Map<String, Object> details;

    /**
     * Duration in seconds of the interaction (if applicable)
     */
    private Integer durationSeconds;

    /**
     * IP address from which the interaction originated
     */
    private String ipAddress;

    /**
     * User agent string from the device
     */
    private String userAgent;

    /**
     * Whether this interaction led to a conversion
     */
    @Builder.Default
    private Boolean isConversion = false;
}
