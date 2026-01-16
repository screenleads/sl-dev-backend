package com.screenleads.backend.app.domain.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.screenleads.backend.app.domain.model.PromotionRedemption;
import com.screenleads.backend.app.domain.model.CouponStatus;
import com.screenleads.backend.app.domain.model.FraudStatus;
import com.screenleads.backend.app.domain.model.RedemptionBillingStatus;

public interface PromotionRedemptionRepository extends JpaRepository<PromotionRedemption, Long> {
    
    Optional<PromotionRedemption> findByCouponCode(String couponCode);
    
    List<PromotionRedemption> findByCustomerId(Long customerId);
    
    List<PromotionRedemption> findByPromotionId(Long promotionId);
    
    List<PromotionRedemption> findByDeviceId(Long deviceId);
    
    List<PromotionRedemption> findByCouponStatus(CouponStatus status);
    
    List<PromotionRedemption> findByFraudStatus(FraudStatus status);
    
    List<PromotionRedemption> findByBillingStatus(RedemptionBillingStatus status);
    
    @Query("SELECT pr FROM PromotionRedemption pr WHERE pr.promotion.id = :promotionId AND pr.customer.id = :customerId")
    List<PromotionRedemption> findByPromotionAndCustomer(@Param("promotionId") Long promotionId, @Param("customerId") Long customerId);
    
    @Query("SELECT COUNT(pr) FROM PromotionRedemption pr WHERE pr.promotion.id = :promotionId AND pr.customer.id = :customerId")
    Long countByPromotionAndCustomer(@Param("promotionId") Long promotionId, @Param("customerId") Long customerId);
    
    @Query("SELECT pr FROM PromotionRedemption pr WHERE pr.promotion.id = :promotionId AND pr.customer.id = :customerId AND pr.createdAt >= :since")
    List<PromotionRedemption> findRecentRedemptionsByPromotionAndCustomer(
        @Param("promotionId") Long promotionId, 
        @Param("customerId") Long customerId,
        @Param("since") Instant since
    );
    
    @Query("SELECT COUNT(pr) FROM PromotionRedemption pr WHERE pr.promotion.id = :promotionId AND pr.verified = true")
    Long countVerifiedRedemptionsByPromotion(@Param("promotionId") Long promotionId);
    
    @Query("SELECT pr FROM PromotionRedemption pr WHERE pr.billingStatus = :status AND pr.createdAt BETWEEN :startDate AND :endDate")
    List<PromotionRedemption> findByBillingStatusAndDateRange(
        @Param("status") RedemptionBillingStatus status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );
}
