package com.screenleads.backend.app.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

/**
 * Representa el canje de una promoción por un usuario final.
 * Reemplaza la antigua entidad PromotionLead con un modelo más completo.
 */
@Entity
@Table(
    name = "promotion_redemption",
    indexes = {
        @Index(name = "ix_redemption_promotion", columnList = "promotion_id"),
        @Index(name = "ix_redemption_customer", columnList = "customer_id"),
        @Index(name = "ix_redemption_device", columnList = "device_id"),
        @Index(name = "ix_redemption_created", columnList = "created_at"),
        @Index(name = "ix_redemption_status", columnList = "coupon_status"),
        @Index(name = "ix_redemption_billing", columnList = "billing_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRedemption extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Relaciones ===
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_redemption_promotion"))
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_redemption_customer"))
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_redemption_device"))
    private Device device; // En qué dispositivo se canjeó

    // === Cupón generado ===
    @Column(name = "coupon_code", unique = true, length = 64, nullable = false)
    private String couponCode; // Código único generado

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_status", length = 20, nullable = false)
    @Builder.Default
    private CouponStatus couponStatus = CouponStatus.VALID;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt; // Cuándo se usó en el negocio externo

    // === Datos del canje ===
    @Enumerated(EnumType.STRING)
    @Column(name = "redemption_method", length = 30)
    private AuthMethod redemptionMethod; // Cómo se registró

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // IPv4 o IPv6

    @Lob
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    // === Validación y verificación ===
    @Builder.Default
    private Boolean verified = false;
    
    private Instant verifiedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", length = 30)
    private VerificationMethod verificationMethod;

    // === Calidad y fraude ===
    @Column(name = "lead_score")
    private Integer leadScore; // 0-100

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_status", length = 30)
    @Builder.Default
    private FraudStatus fraudStatus = FraudStatus.PENDING;

    @Lob
    @Column(name = "fraud_check_details", columnDefinition = "TEXT")
    private String fraudCheckDetails; // JSON con detalles

    // === Tracking de origen ===
    @Column(name = "utm_source", length = 100)
    private String utmSource;

    @Column(name = "utm_medium", length = 100)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 100)
    private String utmCampaign;

    @Column(name = "utm_content", length = 100)
    private String utmContent;

    @Column(name = "referral_code", length = 50)
    private String referralCode;

    // === Métricas de interacción ===
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds; // Tiempo que tardó en el formulario

    @Column(name = "form_interactions")
    private Integer formInteractions; // Cuántos campos tocó

    @Builder.Default
    @Column(name = "attempts")
    private Integer attempts = 1; // Intentos hasta completar

    // === Estado de facturación ===
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_status", length = 30)
    @Builder.Default
    private RedemptionBillingStatus billingStatus = RedemptionBillingStatus.PENDING;

    @Column(name = "billed_at")
    private Instant billedAt;

    @Column(name = "billing_amount", precision = 10, scale = 2)
    private BigDecimal billingAmount;

    // === Metadata adicional ===
    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON con datos adicionales
    
    // === Métodos helper ===
    
    /**
     * Verifica si el cupón puede ser usado
     */
    public boolean canBeRedeemed() {
        if (couponStatus != CouponStatus.VALID) {
            return false;
        }
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }
    
    /**
     * Marca el cupón como usado
     */
    public void markAsRedeemed() {
        this.couponStatus = CouponStatus.REDEEMED;
        this.redeemedAt = Instant.now();
    }
    
    /**
     * Marca el cupón como expirado
     */
    public void markAsExpired() {
        this.couponStatus = CouponStatus.EXPIRED;
    }
    
    /**
     * Marca el cupón como cancelado
     */
    public void markAsCancelled() {
        this.couponStatus = CouponStatus.CANCELLED;
    }
    
    /**
     * Verifica si el canje está verificado y limpio (no fraudulento)
     */
    public boolean isValidLead() {
        return Boolean.TRUE.equals(verified) 
            && (fraudStatus == FraudStatus.CLEAN || fraudStatus == FraudStatus.PENDING);
    }
    
    /**
     * Verifica si el canje ya fue facturado
     */
    public boolean isBilled() {
        return billingStatus == RedemptionBillingStatus.BILLED;
    }
}
