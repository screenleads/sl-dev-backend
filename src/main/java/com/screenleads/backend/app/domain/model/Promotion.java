package com.screenleads.backend.app.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

/**
 * Promoción que puede ser canjeada por usuarios finales.
 * Contiene todas las reglas de negocio, límites, presupuestos y configuración.
 */
@Entity
@Table(name = "promotion", indexes = {
        @Index(name = "ix_promotion_company", columnList = "company_id"),
        @Index(name = "ix_promotion_status", columnList = "status"),
        @Index(name = "ix_promotion_dates", columnList = "start_at, end_at"),
        @Index(name = "ix_promotion_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_promotion_company"))
    private Company company;

    // === Información básica ===
    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "short_description", length = 255)
    private String shortDescription; // Para listados

    // === URLs ===
    @Column(name = "terms_url", length = 2048)
    private String termsUrl; // Condiciones legales

    @Column(name = "privacy_url", length = 2048)
    private String privacyUrl;

    @Column(name = "external_redemption_url", length = 2048)
    private String externalRedemptionUrl; // URL externa para canjear

    @Column(name = "landing_url", length = 2048)
    private String landingUrl; // Landing page de la promoción

    // === Templates ===
    @Lob
    @Column(name = "template_html", columnDefinition = "TEXT")
    private String templateHtml; // Template personalizado del formulario

    @Lob
    @Column(name = "email_template_html", columnDefinition = "TEXT")
    private String emailTemplateHtml; // Template de email de confirmación

    @Lob
    @Column(name = "success_message", columnDefinition = "TEXT")
    private String successMessage; // Mensaje tras canjear

    // === Cupón externo ===
    @Column(name = "external_coupon_code", length = 120)
    private String externalCouponCode; // Código para canjear en negocio externo

    @Column(name = "coupon_prefix", length = 20)
    private String couponPrefix; // Prefijo para cupones generados: "SCREEN2024-"

    // === Ventana temporal ===
    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Europe/Madrid";

    // === Reglas de identificación ===
    @Enumerated(EnumType.STRING)
    @Column(name = "required_identifier", length = 30, nullable = false)
    @Builder.Default
    private RequiredIdentifier requiredIdentifier = RequiredIdentifier.EMAIL;

    @Builder.Default
    @Column(name = "require_phone")
    private Boolean requirePhone = false;

    @Builder.Default
    @Column(name = "require_email")
    private Boolean requireEmail = true;

    @Builder.Default
    @Column(name = "require_birthdate")
    private Boolean requireBirthdate = false;

    @Builder.Default
    @Column(name = "require_full_name")
    private Boolean requireFullName = true;

    // === Reglas de límites de canje ===
    @Enumerated(EnumType.STRING)
    @Column(name = "redemption_limit_type", length = 30, nullable = false)
    @Builder.Default
    private RedemptionLimitType redemptionLimitType = RedemptionLimitType.ONE_PER_USER;

    // Si redemptionLimitType = TIME_LIMITED_PER_USER
    @Column(name = "redemption_interval_hours")
    private Integer redemptionIntervalHours; // Horas entre canjes del mismo usuario

    // Límite global de canjes
    @Column(name = "max_total_redemptions")
    private Integer maxTotalRedemptions;

    @Column(name = "max_redemptions_per_day")
    private Integer maxRedemptionsPerDay;

    @Column(name = "max_redemptions_per_user")
    private Integer maxRedemptionsPerUser;

    // === Presupuesto y pricing ===
    @Column(name = "budget_total", precision = 10, scale = 2)
    private BigDecimal budgetTotal;

    @Column(name = "budget_spent", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal budgetSpent = BigDecimal.ZERO;

    @Column(name = "price_per_lead", precision = 10, scale = 2)
    private BigDecimal pricePerLead; // Precio específico de esta promo (override)

    // === Geo-targeting ===
    @Lob
    @Column(name = "target_countries", columnDefinition = "TEXT")
    private String targetCountries; // JSON: ["ES", "PT", "FR"]

    @Lob
    @Column(name = "target_regions", columnDefinition = "TEXT")
    private String targetRegions; // JSON: ["Madrid", "Barcelona"]

    @Lob
    @Column(name = "target_cities", columnDefinition = "TEXT")
    private String targetCities; // JSON: ["Madrid", "Barcelona"]

    // === Targeting por dispositivo ===
    @Lob
    @Column(name = "target_device_types", columnDefinition = "TEXT")
    private String targetDeviceTypes; // JSON: ["TABLET", "KIOSK"]

    @Lob
    @Column(name = "target_device_ids", columnDefinition = "TEXT")
    private String targetDeviceIds; // JSON: [1, 5, 7] - devices específicos

    // === Prioridad y estado ===
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0; // Mayor prioridad = se muestra más

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.DRAFT;

    @Builder.Default
    private Boolean active = true;

    // === Imágenes ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_id", foreignKey = @ForeignKey(name = "fk_promotion_thumbnail"))
    private Media thumbnail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_id", foreignKey = @ForeignKey(name = "fk_promotion_banner"))
    private Media banner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_id", foreignKey = @ForeignKey(name = "fk_promotion_background"))
    private Media background;

    // === Métricas (desnormalizadas) ===
    @Column(name = "redemption_count")
    @Builder.Default
    private Integer redemptionCount = 0;

    @Column(name = "unique_users_count")
    @Builder.Default
    private Integer uniqueUsersCount = 0;

    @Column(name = "impression_count")
    @Builder.Default
    private Long impressionCount = 0L;

    @Column(name = "click_count")
    @Builder.Default
    private Long clickCount = 0L;

    // === Relaciones ===
    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PromotionRedemption> redemptions;

    // === Métodos helper ===

    /**
     * Verifica si la promoción está activa y puede ser canjeada
     */
    public boolean isActive() {
        if (!Boolean.TRUE.equals(this.active) || this.status != PromotionStatus.ACTIVE) {
            return false;
        }

        Instant now = Instant.now();
        if (startAt != null && now.isBefore(startAt)) {
            return false;
        }
        if (endAt != null && now.isAfter(endAt)) {
            return false;
        }

        if (maxTotalRedemptions != null && redemptionCount >= maxTotalRedemptions) {
            return false;
        }

        if (budgetTotal != null && budgetSpent.compareTo(budgetTotal) >= 0) {
            return false;
        }

        return true;
    }

    /**
     * Incrementa el contador de canjes y actualiza presupuesto
     */
    public void incrementRedemption(BigDecimal cost) {
        this.redemptionCount++;
        if (cost != null) {
            this.budgetSpent = this.budgetSpent.add(cost);
        }

        // Auto-pausar si se alcanzó el límite
        if (maxTotalRedemptions != null && redemptionCount >= maxTotalRedemptions) {
            this.status = PromotionStatus.COMPLETED;
        }

        // Auto-pausar si se agotó el presupuesto
        if (budgetTotal != null && budgetSpent.compareTo(budgetTotal) >= 0) {
            this.status = PromotionStatus.BUDGET_EXHAUSTED;
        }
    }

    /**
     * Incrementa el contador de impresiones
     */
    public void incrementImpression() {
        this.impressionCount++;
    }

    /**
     * Incrementa el contador de clics
     */
    public void incrementClick() {
        this.clickCount++;
    }

    /**
     * Calcula la tasa de conversión (redemptions / clicks)
     */
    public BigDecimal getConversionRate() {
        if (clickCount == null || clickCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(redemptionCount != null ? redemptionCount : 0)
                .divide(BigDecimal.valueOf(clickCount), 4, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el CTR (Click-Through Rate): clicks / impressions
     */
    public BigDecimal getCTR() {
        if (impressionCount == null || impressionCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(clickCount != null ? clickCount : 0)
                .divide(BigDecimal.valueOf(impressionCount), 4, RoundingMode.HALF_UP);
    }

    /**
     * Retorna el presupuesto restante
     */
    public BigDecimal getRemainingBudget() {
        if (budgetTotal == null) {
            return null;
        }
        return budgetTotal.subtract(budgetSpent);
    }

    /**
     * Retorna el porcentaje de presupuesto gastado (0-100)
     */
    public BigDecimal getBudgetUsagePercentage() {
        if (budgetTotal == null || budgetTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return budgetSpent
                .divide(budgetTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
