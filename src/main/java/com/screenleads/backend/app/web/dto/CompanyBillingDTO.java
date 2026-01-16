package com.screenleads.backend.app.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.screenleads.backend.app.domain.model.BillingStatus;
import com.screenleads.backend.app.domain.model.PlanType;

public record CompanyBillingDTO(
    Long id,
    Long companyId,
    String companyName,
    String stripeCustomerId,
    String stripeSubscriptionId,
    String stripeSubscriptionItemId,
    BillingStatus billingStatus,
    String legalName,
    String taxId,
    String billingEmail,
    String billingPhone,
    String billingAddress,
    String billingAddress2,
    String billingCity,
    String billingState,
    String billingPostalCode,
    String billingCountry,
    PlanType planType,
    BigDecimal basePrice,
    BigDecimal pricePerLead,
    Integer monthlyLeadQuota,
    Integer maxDevices,
    Integer maxPromotions,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    Integer currentPeriodLeads,
    BigDecimal currentPeriodAmount,
    Long totalLeadsBilled,
    BigDecimal totalAmountBilled,
    Instant lastInvoiceDate,
    Boolean active,
    Instant suspendedAt,
    String suspensionReason,
    String paymentMethodType,
    String paymentMethodLast4,
    String invoiceEmails,
    Instant createdAt,
    Instant updatedAt
) {
}
