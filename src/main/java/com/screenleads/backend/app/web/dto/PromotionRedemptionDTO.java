package com.screenleads.backend.app.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.screenleads.backend.app.domain.model.AuthMethod;
import com.screenleads.backend.app.domain.model.CouponStatus;
import com.screenleads.backend.app.domain.model.FraudStatus;
import com.screenleads.backend.app.domain.model.RedemptionBillingStatus;
import com.screenleads.backend.app.domain.model.VerificationMethod;

public record PromotionRedemptionDTO(
        Long id,
        Long promotionId,
        String promotionName,
        Long customerId,
        String customerEmail,
        Long deviceId,
        String deviceName,
        String couponCode,
        CouponStatus couponStatus,
        Instant expiresAt,
        Instant redeemedAt,
        AuthMethod redemptionMethod,
        String ipAddress,
        String userAgent,
        String sessionId,
        Boolean verified,
        Instant verifiedAt,
        VerificationMethod verificationMethod,
        Integer leadScore,
        FraudStatus fraudStatus,
        String fraudCheckDetails,
        String utmSource,
        String utmMedium,
        String utmCampaign,
        String utmContent,
        String referralCode,
        Integer timeSpentSeconds,
        Integer formInteractions,
        Integer attempts,
        RedemptionBillingStatus billingStatus,
        Instant billedAt,
        BigDecimal billingAmount,
        String metadata,
        Instant createdAt,
        Instant updatedAt) {
}
