package com.screenleads.backend.app.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

/**
 * Datos de facturación y configuración de Stripe para una empresa.
 * Centraliza toda la información necesaria para facturación y cobros.
 */
@Entity
@Table(name = "company_billing",
    indexes = {
        @Index(name = "ix_billing_company", columnList = "company_id", unique = true),
        @Index(name = "ix_billing_status", columnList = "billing_status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyBilling extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true,
        foreignKey = @ForeignKey(name = "fk_billing_company"))
    private Company company;

    // === Stripe ===
    @Column(name = "stripe_customer_id", length = 64)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 64)
    private String stripeSubscriptionId;

    @Column(name = "stripe_subscription_item_id", length = 64)
    private String stripeSubscriptionItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_status", length = 32, nullable = false)
    @Builder.Default
    private BillingStatus billingStatus = BillingStatus.INCOMPLETE;

    // === Datos fiscales ===
    @Column(name = "legal_name", length = 255)
    private String legalName; // Razón social

    @Column(name = "tax_id", length = 50)
    private String taxId; // NIF/CIF/VAT

    @Column(name = "billing_email", length = 320)
    private String billingEmail;

    @Column(name = "billing_phone", length = 50)
    private String billingPhone;

    // === Dirección de facturación ===
    @Column(name = "billing_address", length = 255)
    private String billingAddress;

    @Column(name = "billing_address_2", length = 255)
    private String billingAddress2;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state", length = 100)
    private String billingState;

    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;

    @Column(name = "billing_country", length = 2)
    private String billingCountry; // ISO 3166-1 alpha-2

    // === Plan y límites ===
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", length = 30)
    @Builder.Default
    private PlanType planType = PlanType.METERED;

    @Column(name = "base_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal basePrice = BigDecimal.ZERO; // Precio base mensual

    @Column(name = "price_per_lead", precision = 10, scale = 2)
    private BigDecimal pricePerLead; // Precio por lead (puede ser custom)

    @Column(name = "monthly_lead_quota")
    private Integer monthlyLeadQuota; // Cuota incluida en plan base

    @Column(name = "max_devices")
    private Integer maxDevices;

    @Column(name = "max_promotions")
    private Integer maxPromotions;

    // === Uso actual ===
    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "current_period_leads")
    @Builder.Default
    private Integer currentPeriodLeads = 0;

    @Column(name = "current_period_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal currentPeriodAmount = BigDecimal.ZERO;

    // === Histórico ===
    @Column(name = "total_leads_billed")
    @Builder.Default
    private Long totalLeadsBilled = 0L;

    @Column(name = "total_amount_billed", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmountBilled = BigDecimal.ZERO;

    @Column(name = "last_invoice_date")
    private Instant lastInvoiceDate;

    // === Estado ===
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "suspended_at")
    private Instant suspendedAt;
    
    @Column(name = "suspension_reason", length = 255)
    private String suspensionReason;

    // === Métodos de pago ===
    @Column(name = "payment_method_type", length = 30)
    private String paymentMethodType; // card, sepa_debit, etc.

    @Column(name = "payment_method_last4", length = 4)
    private String paymentMethodLast4;

    // === Notificaciones ===
    @Lob
    @Column(name = "invoice_emails", columnDefinition = "TEXT")
    private String invoiceEmails; // JSON: ["billing@company.com", "cfo@company.com"]

    // === Relaciones ===
    @OneToMany(mappedBy = "companyBilling", cascade = CascadeType.ALL)
    private List<Invoice> invoices;

    @OneToMany(mappedBy = "companyBilling", cascade = CascadeType.ALL)
    private List<BillingEvent> events;
    
    // === Métodos helper ===
    
    /**
     * Incrementa el uso del periodo actual
     */
    public void incrementCurrentPeriodUsage(int leads, BigDecimal amount) {
        this.currentPeriodLeads += leads;
        this.currentPeriodAmount = this.currentPeriodAmount.add(amount);
    }
    
    /**
     * Resetea el uso del periodo (se llama al inicio de cada periodo de facturación)
     */
    public void resetCurrentPeriod(Instant periodStart, Instant periodEnd) {
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        this.currentPeriodLeads = 0;
        this.currentPeriodAmount = BigDecimal.ZERO;
    }
    
    /**
     * Actualiza el histórico de facturación
     */
    public void updateBillingHistory(long leads, BigDecimal amount) {
        this.totalLeadsBilled += leads;
        this.totalAmountBilled = this.totalAmountBilled.add(amount);
        this.lastInvoiceDate = Instant.now();
    }
    
    /**
     * Verifica si la empresa ha alcanzado los límites de su plan
     */
    public boolean hasReachedDeviceLimit(int currentDevices) {
        return maxDevices != null && currentDevices >= maxDevices;
    }
    
    /**
     * Verifica si la empresa ha alcanzado el límite de promociones
     */
    public boolean hasReachedPromotionLimit(int currentPromotions) {
        return maxPromotions != null && currentPromotions >= maxPromotions;
    }
    
    /**
     * Calcula leads restantes en la cuota mensual
     */
    public Integer getRemainingLeadQuota() {
        if (monthlyLeadQuota == null) {
            return null;
        }
        return Math.max(0, monthlyLeadQuota - currentPeriodLeads);
    }
    
    /**
     * Verifica si la facturación está activa y configurada
     */
    public boolean isFullyConfigured() {
        return stripeCustomerId != null 
            && stripeSubscriptionId != null 
            && billingStatus == BillingStatus.ACTIVE
            && Boolean.TRUE.equals(active);
    }
}
