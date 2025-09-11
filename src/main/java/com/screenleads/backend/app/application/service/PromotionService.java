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

    // Leads
    PromotionLeadDTO registerLead(Long promotionId, PromotionLeadDTO dto);

    List<PromotionLeadDTO> listLeads(Long promotionId);

    // Informes / export
    String exportLeadsCsv(Long promotionId, ZonedDateTime from, ZonedDateTime to);

    LeadSummaryDTO getLeadSummary(Long promotionId, ZonedDateTime from, ZonedDateTime to);

    // Lead de prueba
    PromotionLeadDTO createTestLead(Long promotionId, PromotionLeadDTO overrides);
}
