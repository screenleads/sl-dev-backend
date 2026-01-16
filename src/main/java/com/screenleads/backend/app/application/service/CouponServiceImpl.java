package com.screenleads.backend.app.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.application.service.util.CouponCodeGenerator;
import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRedemptionRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private static final String COUPON_NOT_FOUND = "Coupon not found";
    private static final String PROMOTION_NOT_FOUND = "Promotion not found: ";
    private static final String CUSTOMER_NOT_FOUND = "Customer not found: ";

    private final PromotionRepository promotionRepository;
    private final PromotionRedemptionRepository promotionRedemptionRepository;
    private final CustomerRepository customerRepository;

    @Override
    public List<PromotionRedemption> getAllCoupons() {
        return promotionRedemptionRepository.findAll();
    }

    @Override
    public PromotionRedemption issueCoupon(Long promotionId, Long customerId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException(PROMOTION_NOT_FOUND + promotionId));

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(CUSTOMER_NOT_FOUND + customerId));

        // Chequeo de ventana temporal de la promo
        Instant now = Instant.now();
        if (promotion.getStartAt() != null && now.isBefore(promotion.getStartAt())) {
            throw new IllegalStateException("Promotion not started yet");
        }
        if (promotion.getEndAt() != null && now.isAfter(promotion.getEndAt())) {
            throw new IllegalStateException("Promotion already ended");
        }

        // Enforce límites
        RedemptionLimitType limit = promotion.getRedemptionLimitType();
        if (limit == RedemptionLimitType.ONE_PER_PERSON) {
            long count = promotionRedemptionRepository.countByPromotionIdAndCustomerId(promotionId, customerId);
            if (count > 0) {
                throw new IllegalStateException("Limit reached: ONE_PER_PERSON");
            }
        } else if (limit == RedemptionLimitType.ONE_PER_24H) {
            Instant since = now.minus(24, ChronoUnit.HOURS);
            long count = promotionRedemptionRepository.countByPromotionIdAndCustomerIdAndRedeemedAtAfter(promotionId, customerId, since);
            if (count > 0) {
                throw new IllegalStateException("Limit reached: ONE_PER_24H");
            }
        }

        // Generar código único
        String code;
        do {
            code = CouponCodeGenerator.generate(12);
        } while (promotionRedemptionRepository.findByCouponCode(code).isPresent());

        // Crear redemption
        PromotionRedemption redemption = new PromotionRedemption();
        redemption.setPromotion(promotion);
        redemption.setCustomer(customer);
        redemption.setRequiredIdentifierType(promotion.getRequiredIdentifierType());
        redemption.setIdentifierValue(customer.getEmail()); // o el identifier adecuado
        redemption.setCouponCode(code);
        redemption.setCouponStatus(CouponStatus.VALID);
        redemption.setExpiresAt(promotion.getEndAt());

        return promotionRedemptionRepository.save(redemption);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionRedemption validate(String couponCode) {
        PromotionRedemption redemption = promotionRedemptionRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new IllegalArgumentException(COUPON_NOT_FOUND));

        Instant now = Instant.now();
        Promotion p = redemption.getPromotion();

        if (redemption.getCouponStatus() == CouponStatus.CANCELLED) {
            throw new IllegalStateException("Coupon cancelled");
        }
        if (redemption.getCouponStatus() == CouponStatus.REDEEMED) {
            throw new IllegalStateException("Coupon already redeemed");
        }
        if (redemption.getCouponStatus() == CouponStatus.EXPIRED) {
            throw new IllegalStateException("Coupon expired");
        }
        if (redemption.getExpiresAt() != null && now.isAfter(redemption.getExpiresAt())) {
            throw new IllegalStateException("Coupon expired");
        }
        if (p.getStartAt() != null && now.isBefore(p.getStartAt())) {
            throw new IllegalStateException("Promotion not started yet");
        }
        if (p.getEndAt() != null && now.isAfter(p.getEndAt())) {
            throw new IllegalStateException("Promotion already ended");
        }
        return redemption; // válido
    }

    @Override
    public PromotionRedemption redeem(String couponCode) {
        PromotionRedemption redemption = promotionRedemptionRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new IllegalArgumentException(COUPON_NOT_FOUND));

        // Validaciones reutilizando validate() (pero con escritura)
        Promotion p = redemption.getPromotion();
        Instant now = Instant.now();

        if (redemption.getCouponStatus() == CouponStatus.REDEEMED) {
            throw new IllegalStateException("Coupon already redeemed");
        }
        if (redemption.getCouponStatus() == CouponStatus.CANCELLED) {
            throw new IllegalStateException("Coupon cancelled");
        }
        if (redemption.getCouponStatus() == CouponStatus.EXPIRED) {
            throw new IllegalStateException("Coupon expired");
        }
        if (redemption.getExpiresAt() != null && now.isAfter(redemption.getExpiresAt())) {
            redemption.setCouponStatus(CouponStatus.EXPIRED);
            promotionRedemptionRepository.save(redemption);
            throw new IllegalStateException("Coupon expired");
        }
        if (p.getStartAt() != null && now.isBefore(p.getStartAt())) {
            throw new IllegalStateException("Promotion not started yet");
        }
        if (p.getEndAt() != null && now.isAfter(p.getEndAt())) {
            redemption.setCouponStatus(CouponStatus.EXPIRED);
            promotionRedemptionRepository.save(redemption);
            throw new IllegalStateException("Promotion already ended");
        }

        redemption.setCouponStatus(CouponStatus.REDEEMED);
        redemption.setRedeemedAt(now);
        return promotionRedemptionRepository.save(redemption);
    }

    @Override
    public PromotionRedemption expire(String couponCode) {
        PromotionRedemption redemption = promotionRedemptionRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new IllegalArgumentException(COUPON_NOT_FOUND));

        if (redemption.getCouponStatus() == CouponStatus.REDEEMED) {
            throw new IllegalStateException("Cannot expire a redeemed coupon");
        }

        redemption.setCouponStatus(CouponStatus.EXPIRED);
        if (redemption.getExpiresAt() == null) {
            redemption.setExpiresAt(Instant.now());
        }
        return promotionRedemptionRepository.save(redemption);
    }
}

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException(PROMOTION_NOT_FOUND + promotionId));

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(CUSTOMER_NOT_FOUND + customerId));

        // Chequeo de ventana temporal de la promo
        Instant now = Instant.now();
        if (promotion.getStartAt() != null && now.isBefore(promotion.getStartAt())) {
            throw new IllegalStateException("Promotion not started yet");
        }
        if (promotion.getEndAt() != null && now.isAfter(promotion.getEndAt())) {
            throw new IllegalStateException("Promotion already ended");
        }

        // Enforce límites
        LeadLimitType limit = promotion.getLeadLimitType();
        if (limit == LeadLimitType.ONE_PER_PERSON) {
            long count = promotionLeadRepository.countByPromotionIdAndCustomerId(promotionId, customerId);
            if (count > 0) {
                throw new IllegalStateException("Limit reached: ONE_PER_PERSON");
            }
        } else if (limit == LeadLimitType.ONE_PER_24H) {
            Instant since = now.minus(24, ChronoUnit.HOURS);
            long count = promotionLeadRepository.countByPromotionAndCustomerSince(promotionId, customerId, since);
            if (count > 0) {
                throw new IllegalStateException("Limit reached: ONE_PER_24H");
            }
        }

        // Generar código único
        String code;
        do {
            code = CouponCodeGenerator.generate(12);
        } while (promotionLeadRepository.findByCouponCode(code).isPresent());

        // Crear lead (histórico)
        PromotionLead lead = PromotionLead.builder()
                .promotion(promotion)
                .customer(customer)
                .identifierType(promotion.getLeadIdentifierType())
                .identifier(customer.getIdentifier())
                .couponCode(code)
                .couponStatus(CouponStatus.VALID) // lo dejamos ya VALID si la promo está activa
                // opcional: establecer expiresAt = endAt de la promo
                .expiresAt(promotion.getEndAt())
                .build();

        return promotionLeadRepository.save(lead);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionLead validate(String couponCode) {
        PromotionLead lead = promotionLeadRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new IllegalArgumentException(COUPON_NOT_FOUND));

        Instant now = Instant.now();
        Promotion p = lead.getPromotion();

        if (lead.getCouponStatus() == CouponStatus.CANCELLED) {
            throw new IllegalStateException("Coupon cancelled");
        }
        if (lead.getCouponStatus() == CouponStatus.REDEEMED) {
            throw new IllegalStateException("Coupon already redeemed");
        }
        if (lead.getCouponStatus() == CouponStatus.EXPIRED) {
            throw new IllegalStateException("Coupon expired");
        }
        if (lead.getExpiresAt() != null && now.isAfter(lead.getExpiresAt())) {
            throw new IllegalStateException("Coupon expired");
        }
        if (p.getStartAt() != null && now.isBefore(p.getStartAt())) {
            throw new IllegalStateException("Promotion not started yet");
        }
        if (p.getEndAt() != null && now.isAfter(p.getEndAt())) {
            throw new IllegalStateException("Promotion already ended");
        }
        return lead; // válido
    }

    @Override
    public PromotionLead redeem(String couponCode) {
        PromotionLead lead = promotionLeadRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new IllegalArgumentException(COUPON_NOT_FOUND));

        // Validaciones reutilizando validate() (pero con escritura)
        Promotion p = lead.getPromotion();
        Instant now = Instant.now();

        if (lead.getCouponStatus() == CouponStatus.REDEEMED) {
            throw new IllegalStateException("Coupon already redeemed");
        }
        if (lead.getCouponStatus() == CouponStatus.CANCELLED) {
            throw new IllegalStateException("Coupon cancelled");
        }
        if (lead.getCouponStatus() == CouponStatus.EXPIRED) {
            throw new IllegalStateException("Coupon expired");
        }
        if (lead.getExpiresAt() != null && now.isAfter(lead.getExpiresAt())) {
            lead.setCouponStatus(CouponStatus.EXPIRED);
            promotionLeadRepository.save(lead);
            throw new IllegalStateException("Coupon expired");
        }
        if (p.getStartAt() != null && now.isBefore(p.getStartAt())) {
            throw new IllegalStateException("Promotion not started yet");
        }
        if (p.getEndAt() != null && now.isAfter(p.getEndAt())) {
            lead.setCouponStatus(CouponStatus.EXPIRED);
            promotionLeadRepository.save(lead);
            throw new IllegalStateException("Promotion already ended");
        }

        lead.setCouponStatus(CouponStatus.REDEEMED);
        lead.setRedeemedAt(now);
        return promotionLeadRepository.save(lead);
    }

    @Override
    public PromotionLead expire(String couponCode) {
        PromotionLead lead = promotionLeadRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new IllegalArgumentException(COUPON_NOT_FOUND));

        if (lead.getCouponStatus() == CouponStatus.REDEEMED) {
            throw new IllegalStateException("Cannot expire a redeemed coupon");
        }

        lead.setCouponStatus(CouponStatus.EXPIRED);
        if (lead.getExpiresAt() == null) {
            lead.setExpiresAt(Instant.now());
        }
        return promotionLeadRepository.save(lead);
    }
}
