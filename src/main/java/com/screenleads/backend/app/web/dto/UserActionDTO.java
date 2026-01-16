package com.screenleads.backend.app.web.dto;

import java.time.Instant;

import com.screenleads.backend.app.domain.model.UserActionType;

public record UserActionDTO(
    Long id,
    Long customerId,
    String customerEmail,
    Long deviceId,
    String deviceName,
    Instant timestamp,
    UserActionType actionType,
    String entityType,
    Long entityId,
    String ipAddress,
    String userAgent,
    String sessionId,
    String details,
    Double latitude,
    Double longitude,
    String city,
    String country
) {
}
