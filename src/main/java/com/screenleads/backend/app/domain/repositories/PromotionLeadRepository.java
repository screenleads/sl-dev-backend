package com.screenleads.backend.app.domain.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.screenleads.backend.app.domain.model.CouponStatus;
import com.screenleads.backend.app.domain.model.PromotionLead;

public interface PromotionLeadRepository extends JpaRepository<PromotionLead, Long> {

    // ---- Búsquedas básicas ----
    List<PromotionLead> findByPromotionId(Long promotionId);

    Optional<PromotionLead> findByCouponCode(String couponCode);
    boolean existsByCouponCode(String couponCode);

    // Ambos órdenes por conveniencia
    Optional<PromotionLead> findByIdentifierAndPromotionId(String identifier, Long promotionId);
    Optional<PromotionLead> findByPromotionIdAndIdentifier(Long promotionId, String identifier);

    // *** Método que faltaba y causa el fallo de compilación ***
    boolean existsByPromotionIdAndIdentifier(Long promotionId, String identifier);

    Optional<PromotionLead> findTopByPromotionIdAndCustomerIdOrderByCreatedAtDesc(
            Long promotionId, Long customerId);

    // ---- Conteos / límites por cliente y promo ----
    long countByPromotionIdAndCustomerId(Long promotionId, Long customerId);

    long countByPromotionIdAndCustomerIdAndCouponStatus(
            Long promotionId, Long customerId, CouponStatus couponStatus);

    Optional<PromotionLead> findByPromotionIdAndCustomerIdAndCouponStatus(
            Long promotionId, Long customerId, CouponStatus couponStatus);

    // Conteo en rango temporal (útil para ventanas deslizantes)
    @Query("select count(pl) from PromotionLead pl " +
           "where pl.promotion.id = :promotionId " +
           "and pl.customer.id  = :customerId " +
           "and pl.createdAt between :from and :to")
    long countByPromotionIdAndCustomerIdBetweenDates(@Param("promotionId") Long promotionId,
                                                     @Param("customerId") Long customerId,
                                                     @Param("from") Instant from,
                                                     @Param("to") Instant to);

    // Conteo desde un instante (Since = createdAt >= :since)
    @Query("select count(pl) from PromotionLead pl " +
           "where pl.promotion.id = :promotionId " +
           "and pl.customer.id  = :customerId " +
           "and pl.createdAt    >= :since")
    long countByPromotionAndCustomerSince(@Param("promotionId") Long promotionId,
                                          @Param("customerId") Long customerId,
                                          @Param("since") Instant since);
}
