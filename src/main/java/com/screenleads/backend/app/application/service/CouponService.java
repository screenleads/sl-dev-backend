package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.PromotionLead;
import java.util.List;

public interface CouponService {

    // Lista todos los cupones (leads de promoción)
    List<PromotionLead> getAllCoupons();

    // Emite un cupón (crea un lead histórico) para un customer dado
    PromotionLead issueCoupon(Long promotionId, Long customerId);

    // Validación por código (verifica fechas/estado y devuelve el lead)
    PromotionLead validate(String couponCode);

    // Canje (marca REDEEMED si es válido), devuelve lead actualizado
    PromotionLead redeem(String couponCode);

    // Caducar manualmente (o programáticamente)
    PromotionLead expire(String couponCode);
}
