package com.screenleads.backend.app.web.dto;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record PromotionLeadDTO(
        Long id,
        Long promotionId,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate,
        ZonedDateTime acceptedPrivacyAt,
        ZonedDateTime acceptedTermsAt,
        ZonedDateTime createdAt) {
}
