package com.screenleads.backend.app.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import com.screenleads.backend.app.domain.model.AuthMethod;
import com.screenleads.backend.app.domain.model.Gender;
import com.screenleads.backend.app.domain.model.UserSegment;

public record CustomerDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate,
        Gender gender,
        String city,
        String postalCode,
        String country,
        String preferredLanguage,
        Set<AuthMethod> authMethods,
        String socialProfiles,
        Boolean marketingOptIn,
        Instant marketingOptInAt,
        Boolean dataProcessingConsent,
        Instant dataProcessingConsentAt,
        Boolean thirdPartyDataSharing,
        Instant thirdPartyDataSharingAt,
        Boolean emailVerified,
        Instant emailVerifiedAt,
        Boolean phoneVerified,
        Instant phoneVerifiedAt,
        String tags,
        Integer engagementScore,
        UserSegment segment,
        Integer totalRedemptions,
        Integer uniquePromotionsRedeemed,
        BigDecimal lifetimeValue,
        Instant firstInteractionAt,
        Instant lastInteractionAt,
        Instant createdAt,
        Instant updatedAt) {
}
