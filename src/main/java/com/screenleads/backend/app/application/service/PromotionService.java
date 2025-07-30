package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.web.dto.PromotionDTO;
import java.util.List;

public interface PromotionService {
    List<PromotionDTO> getAllPromotions();

    PromotionDTO getPromotionById(Long id);

    PromotionDTO savePromotion(PromotionDTO promotionDTO);

    PromotionDTO updatePromotion(Long id, PromotionDTO promotionDTO);

    void deletePromotion(Long id);
}
