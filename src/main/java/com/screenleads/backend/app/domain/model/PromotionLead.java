package com.screenleads.backend.app.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "promotion_lead",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_promotionlead_promotion_identifier",
            columnNames = {"promotion_id", "identifier"}
        ),
        @UniqueConstraint(
            name = "uk_promotionlead_coupon_code",
            columnNames = {"coupon_code"}
        )
    },
    indexes = {
        @Index(name = "ix_promotionlead_promotion", columnList = "promotion_id"),
        @Index(name = "ix_promotionlead_created_at", columnList = "created_at"),
        @Index(name = "ix_promotionlead_coupon_status", columnList = "coupon_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class PromotionLead extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Relaciones ===
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "promotion_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_promotionlead_promotion")
    )
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "customer_id",
        foreignKey = @ForeignKey(name = "fk_promotionlead_customer")
    )
    private Customer customer;

    // === Datos personales (opcionales) ===
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    private LocalDate birthDate;

    // Consentimientos
    private Instant acceptedPrivacyAt;
    private Instant acceptedTermsAt;

    // === Identificación del lead dentro de la promo ===
    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 20)
    private LeadIdentifierType identifierType;

    @Column(nullable = false, length = 255)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", length = 20)
    private LeadLimitType limitType;

    // === Cupón interno generado por el sistema ===
    @Column(name = "coupon_code", length = 64, nullable = false)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_status", length = 20, nullable = false)
    @Builder.Default
    private CouponStatus couponStatus = CouponStatus.VALID;

    // Cuándo caduca este cupón concreto (si aplica) y cuándo fue canjeado
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;
}
