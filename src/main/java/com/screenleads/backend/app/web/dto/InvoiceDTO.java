package com.screenleads.backend.app.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.screenleads.backend.app.domain.model.InvoiceStatus;

public record InvoiceDTO(
    Long id,
    Long companyBillingId,
    String companyName,
    String invoiceNumber,
    String stripeInvoiceId,
    Instant periodStart,
    Instant periodEnd,
    LocalDate issueDate,
    LocalDate dueDate,
    Instant paidAt,
    BigDecimal subtotal,
    BigDecimal taxRate,
    BigDecimal taxAmount,
    BigDecimal discountAmount,
    BigDecimal total,
    String currency,
    InvoiceStatus status,
    String companyLegalName,
    String companyTaxId,
    String companyAddress,
    String pdfUrl,
    String stripeHostedUrl,
    String notes,
    List<InvoiceItemDTO> items,
    Instant createdAt,
    Instant updatedAt
) {
}
