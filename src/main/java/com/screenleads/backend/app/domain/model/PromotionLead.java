package com.screenleads.backend.app.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "promotion_lead",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_promotionlead_coupon_code",
            columnNames = {"coupon_code"}
        )
    },
    indexes = {
        @Index(name = "ix_promotionlead_promotion", columnList = "promotion_id"),
        @Index(name = "ix_promotionlead_customer", columnList = "customer_id"),
        @Index(name = "ix_promotionlead_identifier", columnList = "identifier"),
        @Index(name = "ix_promotionlead_created_at", columnList = "created_at"),
        @Index(name = "ix_promotionlead_status", columnList = "coupon_status")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromotionLead extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A qué promoción pertenece este canje/lead
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_promotionlead_promotion"))
    private Promotion promotion;

    // Cliente que realizó el canje
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_promotionlead_customer"))
    private Customer customer;

    // Datos personales capturados (opcional, para reporting)
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

    // Identificador normalizado (redundante a Customer, útil para búsqueda/analytics)
    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 20)
    private LeadIdentifierType identifierType;

    @Column(name = "identifier", nullable = false, length = 255)
    private String identifier;

    // === NUEVO: cupón interno único por lead (para QR / barcode)
    @Column(name = "coupon_code", nullable = false, length = 64, unique = true)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_status", nullable = false, length = 20)
    @Builder.Default
    private CouponStatus couponStatus = CouponStatus.NEW;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "expires_at")
    private Instant expiresAt; // si quisieras expiración por cupón individual; opcional
}
