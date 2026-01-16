package com.screenleads.backend.app.web.dto;

import java.time.Instant;

import com.screenleads.backend.app.domain.model.enums.ExportFormat;
import com.screenleads.backend.app.domain.model.enums.ExportStatus;
import com.screenleads.backend.app.domain.model.enums.ExportType;

public record DataExportDTO(
    Long id,
    Long companyId,
    String companyName,
    Long requestedById,
    String requestedByName,
    ExportType exportType,
    ExportFormat exportFormat,
    String filters,
    Integer totalRecords,
    String fileUrl,
    Long fileSizeBytes,
    Instant expiresAt,
    ExportStatus status,
    Instant startedAt,
    Instant completedAt,
    String errorMessage,
    String purpose,
    Instant downloadedAt,
    Integer downloadCount,
    Instant createdAt,
    Instant updatedAt
) {
}
