package com.screenleads.backend.app.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.screenleads.backend.app.domain.model.BillingEventStatus;
import com.screenleads.backend.app.domain.model.BillingEventType;

public record BillingEventDTO(
        Long id,
        Long companyBillingId,
        String companyName,
        Long redemptionId,
        Long invoiceId,
        Instant timestamp,
        BillingEventType eventType,
        String stripeEventId,
        String stripeUsageRecordId,
        Integer quantity,
        BigDecimal amount,
        String currency,
        BillingEventStatus status,
        String errorMessage,
        String metadata) {
}
