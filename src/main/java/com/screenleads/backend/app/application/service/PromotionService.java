package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.web.dto.*;

import java.time.ZonedDateTime;
import java.util.List;

public interface PromotionService {

    // CRUD promotion
    List<PromotionDTO> getAllPromotions();

    PromotionDTO getPromotionById(Long id);

    PromotionDTO savePromotion(PromotionDTO dto);

    PromotionDTO updatePromotion(Long id, PromotionDTO dto);

    void deletePromotion(Long id);

    // Métodos legacy - DEPRECATED: Usar PromotionRedemptionService en su lugar
    @Deprecated
    PromotionLeadDTO registerLead(Long promotionId, PromotionLeadDTO dto);

    @Deprecated
    List<PromotionLeadDTO> listLeads(Long promotionId);

    @Deprecated
    String exportLeadsCsv(Long promotionId, ZonedDateTime from, ZonedDateTime to);

    @Deprecated
    LeadSummaryDTO getLeadSummary(Long promotionId, ZonedDateTime from, ZonedDateTime to);

    @Deprecated
    PromotionLeadDTO createTestLead(Long promotionId, PromotionLeadDTO overrides);
}
