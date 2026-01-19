package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.PromotionRedemption;
import java.util.List;

public interface CouponService {

    // Lista todos los cupones (leads de promoción)
    List<PromotionRedemption> getAllCoupons();

    // Emite un cupón (crea un lead histórico) para un customer dado desde un device
    // específico
    PromotionRedemption issueCoupon(Long promotionId, Long customerId, Long deviceId);

    // Validación por código (verifica fechas/estado y devuelve el lead)
    PromotionRedemption validate(String couponCode);

    // Canje (marca REDEEMED si es válido), devuelve lead actualizado
    PromotionRedemption redeem(String couponCode);

    // Caducar manualmente (o programáticamente)
    PromotionRedemption expire(String couponCode);
}
