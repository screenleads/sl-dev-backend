package com.screenleads.backend.app.web.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.CouponService;
import com.screenleads.backend.app.domain.model.CouponStatus;
import com.screenleads.backend.app.domain.model.PromotionRedemption;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/coupons")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Validación y canje de cupones de promociones")
public class CouponController {

    private final CouponService couponService;

    // === GET /coupons -> listar todos ===
    @GetMapping
    @PreAuthorize("@perm.can('promotion', 'read')")
    @Operation(summary = "Listar todos los cupones", description = "Devuelve todos los promotion redemptions (cupones)")
    public ResponseEntity<List<PromotionRedemption>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    // === GET /coupons/{code} -> validar ===
    @GetMapping("/{code}")
    @PreAuthorize("@perm.can('promotion', 'read')")
    @Operation(summary = "Validar cupón por código", description = "Devuelve el estado y si es válido en este momento")
    public ResponseEntity<CouponValidationResponse> validate(@PathVariable String code) {
        try {
            PromotionRedemption redemption = couponService.validate(code);
            return ResponseEntity.ok(CouponValidationResponse.from(redemption, true, null));
        } catch (Exception ex) {
            log.warn("Error validando cupón {}: {}", code, ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(new CouponValidationResponse(code, false, null, null, null, ex.getMessage()));
        }
    }

    // === POST /coupons/{code}/redeem -> canjear ===
    @PostMapping("/{code}/redeem")
    @PreAuthorize("@perm.can('promotion', 'write')")
    @Operation(summary = "Canjear cupón por código", description = "Marca el cupón como REDEEMED si es válido")
    public ResponseEntity<CouponValidationResponse> redeem(@PathVariable String code) {
        PromotionRedemption redemption = couponService.redeem(code);
        return ResponseEntity.ok(CouponValidationResponse.from(redemption, true, "REDEEMED"));
    }

    // === POST /coupons/{code}/expire -> caducar manualmente ===
    @PostMapping("/{code}/expire")
    @PreAuthorize("@perm.can('promotion', 'write')")
    @Operation(summary = "Caducar cupón por código", description = "Marca el cupón como EXPIRED si aún no se ha canjeado")
    public ResponseEntity<CouponValidationResponse> expire(@PathVariable String code) {
        PromotionRedemption redemption = couponService.expire(code);
        return ResponseEntity.ok(CouponValidationResponse.from(redemption, false, "EXPIRED"));
    }

    // === POST /coupons/issue?promotionId=&customerId= -> emitir ===
    @PostMapping("/issue")
    @PreAuthorize("@perm.can('promotion', 'write')")
    @Operation(summary = "Emitir cupón (crear lead histórico)", description = "Genera un nuevo cupón interno para un cliente y una promoción, respetando límites")
    public ResponseEntity<CouponValidationResponse> issue(
            @RequestParam Long promotionId,
            @RequestParam Long customerId) {
        PromotionRedemption redemption = couponService.issueCoupon(promotionId, customerId);
        return ResponseEntity.ok(CouponValidationResponse.from(redemption, true, "VALID"));
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

        static CouponValidationResponse from(PromotionRedemption redemption, boolean valid, String msg) {
            return new CouponValidationResponse(
                    redemption.getCouponCode(),
                    valid,
                    redemption.getCouponStatus(),
                    redemption.getRedeemedAt(),
                    redemption.getExpiresAt(),
                    msg);
        }
    }
}
