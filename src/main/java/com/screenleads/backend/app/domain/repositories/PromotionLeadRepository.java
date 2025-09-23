package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.PromotionLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionLeadRepository extends JpaRepository<PromotionLead, Long> {

    Optional<PromotionLead> findTopByPromotion_IdAndIdentifierOrderByCreatedAtDesc(Long promotionId, String identifier);
    Optional<PromotionLead> findByCouponCode(String couponCode);

    long countByPromotion_IdAndIdentifierAndCreatedAtAfter(Long promotionId, String identifier, ZonedDateTime after);
    long countByPromotionAndCustomer(Long promotionId, Long customerId);
    long countByPromotionAndCustomerSince(Long promotionId, Long customerId, Instant since);

boolean existsByPromotionIdAndIdentifier(Long promotionId,String identifier);
    
    List<PromotionLead> findByPromotion_IdOrderByCreatedAtDesc(Long promotionId);

    List<PromotionLead> findByPromotion_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long promotionId, ZonedDateTime from, ZonedDateTime to);
            List<PromotionLead> findByPromotionId(Long promotionId);
}
