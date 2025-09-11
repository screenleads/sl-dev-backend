package com.screenleads.backend.app.web.dto;

import java.time.LocalDate;
import java.util.Map;

public record LeadSummaryDTO(
        Long promotionId,
        long totalLeads,
        long uniqueIdentifiers, // únicos según identifier (email/phone normalizado)
        Map<LocalDate, Long> leadsByDay // YYYY-MM-DD -> conteo
) {
}
