package com.screenleads.backend.app.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.screenleads.backend.app.domain.model.InvoiceItemType;

public record InvoiceItemDTO(
        Long id,
        Long invoiceId,
        Long promotionId,
        String promotionName,
        String description,
        InvoiceItemType itemType,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        Instant periodStart,
        Instant periodEnd,
        String metadata) {
}
