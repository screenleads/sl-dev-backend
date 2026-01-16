package com.screenleads.backend.app.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

/**
 * Registro de auditoría completo de eventos de facturación.
 * Permite trazabilidad total de la sincronización con Stripe.
 */
@Entity
@Table(name = "billing_event",
    indexes = {
        @Index(name = "ix_billingevent_billing", columnList = "company_billing_id"),
        @Index(name = "ix_billingevent_redemption", columnList = "redemption_id"),
        @Index(name = "ix_billingevent_invoice", columnList = "invoice_id"),
        @Index(name = "ix_billingevent_timestamp", columnList = "timestamp"),
        @Index(name = "ix_billingevent_type", columnList = "event_type"),
        @Index(name = "ix_billingevent_status", columnList = "status"),
        @Index(name = "ix_billingevent_stripe", columnList = "stripe_event_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_billing_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_billingevent_billing"))
    private CompanyBilling companyBilling;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redemption_id",
        foreignKey = @ForeignKey(name = "fk_billingevent_redemption"))
    private PromotionRedemption redemption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id",
        foreignKey = @ForeignKey(name = "fk_billingevent_invoice"))
    private Invoice invoice;

    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private BillingEventType eventType;

    // === Stripe ===
    @Column(name = "stripe_event_id", length = 64)
    private String stripeEventId;

    @Column(name = "stripe_usage_record_id", length = 64)
    private String stripeUsageRecordId;

    // === Importes ===
    private Integer quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "EUR";

    // === Estado ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BillingEventStatus status;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // === Metadata ===
    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata;
    /* Ejemplo:
    {
      "stripeResponse": {...},
      "retryAttempt": 2,
      "previousStatus": "PENDING"
    }
    */

    // === Métodos helper ===
    
    /**
     * Crea un evento de lead registrado
     */
    public static BillingEvent leadRegistered(
        CompanyBilling billing,
        PromotionRedemption redemption,
        BigDecimal amount
    ) {
        return BillingEvent.builder()
            .companyBilling(billing)
            .redemption(redemption)
            .timestamp(Instant.now())
            .eventType(BillingEventType.LEAD_REGISTERED)
            .quantity(1)
            .amount(amount)
            .status(BillingEventStatus.SUCCESS)
            .build();
    }
    
    /**
     * Crea un evento de uso reportado a Stripe
     */
    public static BillingEvent usageReported(
        CompanyBilling billing,
        Integer quantity,
        BigDecimal amount,
        String stripeUsageRecordId
    ) {
        return BillingEvent.builder()
            .companyBilling(billing)
            .timestamp(Instant.now())
            .eventType(BillingEventType.USAGE_REPORTED)
            .quantity(quantity)
            .amount(amount)
            .stripeUsageRecordId(stripeUsageRecordId)
            .status(BillingEventStatus.SUCCESS)
            .build();
    }
    
    /**
     * Crea un evento de factura creada
     */
    public static BillingEvent invoiceCreated(
        CompanyBilling billing,
        Invoice invoice
    ) {
        return BillingEvent.builder()
            .companyBilling(billing)
            .invoice(invoice)
            .timestamp(Instant.now())
            .eventType(BillingEventType.INVOICE_CREATED)
            .amount(invoice.getTotal())
            .status(BillingEventStatus.SUCCESS)
            .build();
    }
    
    /**
     * Crea un evento de pago exitoso
     */
    public static BillingEvent paymentSucceeded(
        CompanyBilling billing,
        Invoice invoice,
        String stripeEventId
    ) {
        return BillingEvent.builder()
            .companyBilling(billing)
            .invoice(invoice)
            .timestamp(Instant.now())
            .eventType(BillingEventType.PAYMENT_SUCCEEDED)
            .amount(invoice.getTotal())
            .stripeEventId(stripeEventId)
            .status(BillingEventStatus.SUCCESS)
            .build();
    }
    
    /**
     * Crea un evento de pago fallido
     */
    public static BillingEvent paymentFailed(
        CompanyBilling billing,
        Invoice invoice,
        String stripeEventId,
        String errorMessage
    ) {
        return BillingEvent.builder()
            .companyBilling(billing)
            .invoice(invoice)
            .timestamp(Instant.now())
            .eventType(BillingEventType.PAYMENT_FAILED)
            .amount(invoice.getTotal())
            .stripeEventId(stripeEventId)
            .status(BillingEventStatus.FAILED)
            .errorMessage(errorMessage)
            .build();
    }
    
    /**
     * Marca el evento como fallido
     */
    public void markAsFailed(String errorMessage) {
        this.status = BillingEventStatus.FAILED;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Marca el evento para reintento
     */
    public void markForRetry() {
        this.status = BillingEventStatus.RETRY;
    }
}
