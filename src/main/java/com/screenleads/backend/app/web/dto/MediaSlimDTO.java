package com.screenleads.backend.app.web.dto;

import java.time.Instant;

public record MediaSlimDTO(
        Long id,
        String src,
        MediaTypeDTO type,
        Instant createdAt,
        Instant updatedAt) {
}