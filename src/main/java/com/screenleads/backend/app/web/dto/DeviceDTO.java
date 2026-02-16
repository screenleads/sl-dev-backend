
package com.screenleads.backend.app.web.dto;

import java.time.Instant;

public record DeviceDTO(
        Long id, 
        String uuid, 
        String descriptionName, 
        Number width, 
        Number height, 
        DeviceTypeDTO type,
        CompanyRefDTO company,
        Boolean online,
        Instant lastSeenAt,
        Instant lastHeartbeatAt
) {
}
