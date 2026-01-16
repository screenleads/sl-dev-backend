package com.screenleads.backend.app.web.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.PromotionRedemptionService;
import com.screenleads.backend.app.web.dto.PromotionRedemptionDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin
@RestController
@RequestMapping("/promotion-redemptions")
@Tag(name = "Promotion Redemptions", description = "CRUD de canjes de promociones")
public class PromotionRedemptionController {

    private final PromotionRedemptionService redemptionService;

    public PromotionRedemptionController(PromotionRedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('redemption', 'read')")
    @GetMapping
    @Operation(summary = "Listar todos los canjes")
    public ResponseEntity<List<PromotionRedemptionDTO>> getAllRedemptions() {
        return ResponseEntity.ok(redemptionService.getAllRedemptions());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('redemption', 'read')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener canje por ID")
    public ResponseEntity<PromotionRedemptionDTO> getRedemptionById(@PathVariable Long id) {
        return redemptionService.getRedemptionById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('redemption', 'read')")
    @GetMapping("/coupon/{couponCode}")
    @Operation(summary = "Obtener canje por código de cupón")
    public ResponseEntity<PromotionRedemptionDTO> getRedemptionByCouponCode(@PathVariable String couponCode) {
        return redemptionService.getRedemptionByCouponCode(couponCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('redemption', 'read')")
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Listar canjes por cliente")
    public ResponseEntity<List<PromotionRedemptionDTO>> getRedemptionsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(redemptionService.getRedemptionsByCustomer(customerId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('redemption', 'read')")
    @GetMapping("/promotion/{promotionId}")
    @Operation(summary = "Listar canjes por promoción")
    public ResponseEntity<List<PromotionRedemptionDTO>> getRedemptionsByPromotion(@PathVariable Long promotionId) {
        return ResponseEntity.ok(redemptionService.getRedemptionsByPromotion(promotionId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('redemption', 'write')")
    @PostMapping
    @Operation(summary = "Crear nuevo canje", description = "ROLE_ADMIN o permiso redemption:write")
    public ResponseEntity<PromotionRedemptionDTO> createRedemption(@RequestBody PromotionRedemptionDTO dto) {
        return ResponseEntity.ok(redemptionService.createRedemption(dto));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('redemption', 'write')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar canje", description = "ROLE_ADMIN o permiso redemption:write")
    public ResponseEntity<PromotionRedemptionDTO> updateRedemption(@PathVariable Long id, @RequestBody PromotionRedemptionDTO dto) {
        return ResponseEntity.ok(redemptionService.updateRedemption(id, dto));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('redemption', 'write')")
    @PutMapping("/{id}/verify")
    @Operation(summary = "Verificar canje", description = "ROLE_ADMIN o permiso redemption:write")
    public ResponseEntity<PromotionRedemptionDTO> verifyRedemption(@PathVariable Long id) {
        return ResponseEntity.ok(redemptionService.verifyRedemption(id));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('redemption', 'write')")
    @PutMapping("/{id}/redeem")
    @Operation(summary = "Marcar como canjeado", description = "ROLE_ADMIN o permiso redemption:write")
    public ResponseEntity<PromotionRedemptionDTO> markAsRedeemed(@PathVariable Long id) {
        return ResponseEntity.ok(redemptionService.markAsRedeemed(id));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @apiKeyPerm.can('redemption', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar canje", description = "ROLE_ADMIN o permiso redemption:delete")
    public ResponseEntity<Void> deleteRedemption(@PathVariable Long id) {
        redemptionService.deleteRedemption(id);
        return ResponseEntity.noContent().build();
    }
}
