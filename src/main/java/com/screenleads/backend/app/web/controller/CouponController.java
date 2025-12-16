package com.screenleads.backend.app.web.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.CouponService;
import com.screenleads.backend.app.domain.model.CouponStatus;
import com.screenleads.backend.app.domain.model.PromotionLead;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Validación y canje de cupones de promociones")
public class CouponController {

    private final CouponService couponService;

    // === GET /coupons/{code} -> validar ===
    @GetMapping("/{code}")
    @PreAuthorize("@perm.can('coupon', 'read')")
    @Operation(summary = "Validar cupón por código",
               description = "Devuelve el estado y si es válido en este momento")
    public ResponseEntity<CouponValidationResponse> validate(@PathVariable String code) {
        try {
            PromotionLead lead = couponService.validate(code);
            return ResponseEntity.status(HttpStatus.OK).body(CouponValidationResponse.from(lead, true, null));
        } catch (Exception ex) {
            // No filtramos tipos para simplificar: el mensaje explica la causa
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CouponValidationResponse(code, false, null, null, null, ex.getMessage()));
        }
    }

    // === POST /coupons/{code}/redeem -> canjear ===
    @PostMapping("/{code}/redeem")
    @PreAuthorize("@perm.can('coupon', 'update')")
    @Operation(summary = "Canjear cupón por código",
               description = "Marca el cupón como REDEEMED si es válido")
    public ResponseEntity<CouponValidationResponse> redeem(@PathVariable String code) {
        PromotionLead lead = couponService.redeem(code);
        return ResponseEntity.ok(CouponValidationResponse.from(lead, true, "REDEEMED"));
    }

    // === POST /coupons/{code}/expire -> caducar manualmente ===
    @PostMapping("/{code}/expire")
    @PreAuthorize("@perm.can('coupon', 'update')")
    @Operation(summary = "Caducar cupón por código",
               description = "Marca el cupón como EXPIRED si aún no se ha canjeado")
    public ResponseEntity<CouponValidationResponse> expire(@PathVariable String code) {
        PromotionLead lead = couponService.expire(code);
        return ResponseEntity.ok(CouponValidationResponse.from(lead, false, "EXPIRED"));
    }

    // === POST /coupons/issue?promotionId=&customerId= -> emitir ===
    @PostMapping("/issue")
    @PreAuthorize("@perm.can('coupon', 'create')")
    @Operation(summary = "Emitir cupón (crear lead histórico)",
               description = "Genera un nuevo cupón interno para un cliente y una promoción, respetando límites")
    public ResponseEntity<CouponValidationResponse> issue(
            @RequestParam Long promotionId,
            @RequestParam Long customerId) {
        PromotionLead lead = couponService.issueCoupon(promotionId, customerId);
        return ResponseEntity.ok(CouponValidationResponse.from(lead, true, "VALID"));
    }

    // ====== DTO de respuesta simple ======
    @Data
    @AllArgsConstructor
    static class CouponValidationResponse {
        private String couponCode;
        private boolean valid;
        private CouponStatus status;
        private Instant redeemedAt;
        private Instant expiresAt;
        private String message;

        static CouponValidationResponse from(PromotionLead lead, boolean valid, String msg) {
            return new CouponValidationResponse(
                lead.getCouponCode(),
                valid,
                lead.getCouponStatus(),
                lead.getRedeemedAt(),
                lead.getExpiresAt(),
                msg
            );
        }
    }
}
