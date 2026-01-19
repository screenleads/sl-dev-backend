package com.screenleads.backend.app.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.domain.model.RedemptionBillingStatus;
import com.screenleads.backend.app.web.dto.PromotionRedemptionDTO;

public interface PromotionRedemptionService {

    List<PromotionRedemptionDTO> getAllRedemptions();

    Optional<PromotionRedemptionDTO> getRedemptionById(Long id);

    Optional<PromotionRedemptionDTO> getRedemptionByCouponCode(String couponCode);

    List<PromotionRedemptionDTO> getRedemptionsByCustomer(Long customerId);

    List<PromotionRedemptionDTO> getRedemptionsByPromotion(Long promotionId);

    List<PromotionRedemptionDTO> getRedemptionsByDevice(Long deviceId);

    PromotionRedemptionDTO createRedemption(PromotionRedemptionDTO dto);

    PromotionRedemptionDTO updateRedemption(Long id, PromotionRedemptionDTO dto);

    void deleteRedemption(Long id);

    PromotionRedemptionDTO verifyRedemption(Long id);

    PromotionRedemptionDTO markAsRedeemed(Long id);

    List<PromotionRedemptionDTO> getPendingBillingRedemptions(Instant startDate, Instant endDate);

    PromotionRedemptionDTO updateBillingStatus(Long id, RedemptionBillingStatus status);
}
