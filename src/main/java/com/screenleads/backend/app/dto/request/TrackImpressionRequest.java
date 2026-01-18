package com.screenleads.backend.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for tracking an Advice impression
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackImpressionRequest {

    @NotNull(message = "Advice ID is required")
    private Long adviceId;

    @NotNull(message = "Device ID is required")
    private Long deviceId;

    /**
     * Optional: Customer ID if the user is identified
     */
    private Long customerId;

    /**
     * Duration in seconds that the Advice was displayed
     */
    private Integer durationSeconds;

    /**
     * Session identifier to group impressions
     */
    private String sessionId;

    /**
     * IP address of the device
     */
    private String ipAddress;

    /**
     * Whether the customer interacted with the promotion
     */
    @Builder.Default
    private Boolean wasInteractive = false;

    /**
     * User agent string from the device
     */
    private String userAgent;

    /**
     * Geographic location (latitude)
     */
    private Double latitude;

    /**
     * Geographic location (longitude)
     */
    private Double longitude;
}
