package com.screenleads.backend.app.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.RedemptionBillingStatus;
import com.screenleads.backend.app.web.dto.PromotionRedemptionDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementación temporal de PromotionRedemptionService.
 * TODO: Implementar completamente según requisitos de canje de promociones.
 */
@Service
@Slf4j
public class PromotionRedemptionServiceImpl implements PromotionRedemptionService {

    @Override
    public List<PromotionRedemptionDTO> getAllRedemptions() {
        log.warn("getAllRedemptions() no implementado - retornando lista vacía");
        return List.of();
    }

    @Override
    public Optional<PromotionRedemptionDTO> getRedemptionById(Long id) {
        log.warn("getRedemptionById({}) no implementado - retornando Optional.empty()", id);
        return Optional.empty();
    }

    @Override
    public Optional<PromotionRedemptionDTO> getRedemptionByCouponCode(String couponCode) {
        log.warn("getRedemptionByCouponCode({}) no implementado - retornando Optional.empty()", couponCode);
        return Optional.empty();
    }

    @Override
    public List<PromotionRedemptionDTO> getRedemptionsByCustomer(Long customerId) {
        log.warn("getRedemptionsByCustomer({}) no implementado - retornando lista vacía", customerId);
        return List.of();
    }

    @Override
    public List<PromotionRedemptionDTO> getRedemptionsByPromotion(Long promotionId) {
        log.warn("getRedemptionsByPromotion({}) no implementado - retornando lista vacía", promotionId);
        return List.of();
    }

    @Override
    public List<PromotionRedemptionDTO> getRedemptionsByDevice(Long deviceId) {
        log.warn("getRedemptionsByDevice({}) no implementado - retornando lista vacía", deviceId);
        return List.of();
    }

    @Override
    public PromotionRedemptionDTO createRedemption(PromotionRedemptionDTO dto) {
        log.error("createRedemption() no implementado - lanzando UnsupportedOperationException");
        throw new UnsupportedOperationException("PromotionRedemptionService.createRedemption() no implementado");
    }

    @Override
    public PromotionRedemptionDTO updateRedemption(Long id, PromotionRedemptionDTO dto) {
        log.error("updateRedemption({}) no implementado - lanzando UnsupportedOperationException", id);
        throw new UnsupportedOperationException("PromotionRedemptionService.updateRedemption() no implementado");
    }

    @Override
    public void deleteRedemption(Long id) {
        log.error("deleteRedemption({}) no implementado - lanzando UnsupportedOperationException", id);
        throw new UnsupportedOperationException("PromotionRedemptionService.deleteRedemption() no implementado");
    }

    @Override
    public PromotionRedemptionDTO verifyRedemption(Long id) {
        log.error("verifyRedemption({}) no implementado - lanzando UnsupportedOperationException", id);
        throw new UnsupportedOperationException("PromotionRedemptionService.verifyRedemption() no implementado");
    }

    @Override
    public PromotionRedemptionDTO markAsRedeemed(Long id) {
        log.error("markAsRedeemed({}) no implementado - lanzando UnsupportedOperationException", id);
        throw new UnsupportedOperationException("PromotionRedemptionService.markAsRedeemed() no implementado");
    }

    @Override
    public List<PromotionRedemptionDTO> getPendingBillingRedemptions(Instant startDate, Instant endDate) {
        log.warn("getPendingBillingRedemptions({}, {}) no implementado - retornando lista vacía", startDate, endDate);
        return List.of();
    }

    @Override
    public PromotionRedemptionDTO updateBillingStatus(Long id, RedemptionBillingStatus status) {
        log.error("updateBillingStatus({}, {}) no implementado - lanzando UnsupportedOperationException", id, status);
        throw new UnsupportedOperationException("PromotionRedemptionService.updateBillingStatus() no implementado");
    }
}
