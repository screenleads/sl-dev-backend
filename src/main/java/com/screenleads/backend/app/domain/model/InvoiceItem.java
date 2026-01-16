package com.screenleads.backend.app.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

/**
 * Representa una línea individual dentro de una factura.
 * Puede ser una línea de leads, cuota base, descuento, etc.
 */
@Entity
@Table(name = "invoice_item",
    indexes = {
        @Index(name = "ix_invoiceitem_invoice", columnList = "invoice_id"),
        @Index(name = "ix_invoiceitem_promotion", columnList = "promotion_id"),
        @Index(name = "ix_invoiceitem_type", columnList = "item_type")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_invoiceitem_invoice"))
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id",
        foreignKey = @ForeignKey(name = "fk_invoiceitem_promotion"))
    private Promotion promotion; // Si se desglosa por promoción

    // === Descripción ===
    @Column(nullable = false, length = 255)
    private String description; // "Leads generados - Promoción Verano 2026"

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private InvoiceItemType itemType;

    // === Cantidad y precio ===
    @Column(nullable = false)
    private Integer quantity; // Número de leads

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice; // Precio por lead

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount; // quantity * unitPrice

    // === Periodo ===
    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    // === Metadata ===
    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON con detalles adicionales
    /* Ejemplo:
    {
      "verifiedLeads": 85,
      "suspiciousLeads": 5,
      "fraudLeads": 0,
      "averageLeadScore": 87.5
    }
    */

    // === Métodos helper ===
    
    /**
     * Calcula el importe total basado en cantidad y precio unitario
     */
    public void calculateAmount() {
        if (quantity != null && unitPrice != null) {
            this.amount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
    
    /**
     * Crea una línea de leads facturados
     */
    public static InvoiceItem createLeadsItem(
        Promotion promotion, 
        Integer quantity, 
        BigDecimal unitPrice,
        Instant periodStart,
        Instant periodEnd
    ) {
        InvoiceItem item = InvoiceItem.builder()
            .promotion(promotion)
            .description("Leads generados - " + promotion.getName())
            .itemType(InvoiceItemType.LEADS)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .build();
        
        item.calculateAmount();
        return item;
    }
    
    /**
     * Crea una línea de cuota base
     */
    public static InvoiceItem createBaseFeeItem(
        BigDecimal amount,
        Instant periodStart,
        Instant periodEnd
    ) {
        return InvoiceItem.builder()
            .description("Cuota base mensual")
            .itemType(InvoiceItemType.BASE_FEE)
            .quantity(1)
            .unitPrice(amount)
            .amount(amount)
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .build();
    }
    
    /**
     * Crea una línea de descuento
     */
    public static InvoiceItem createDiscountItem(
        String description,
        BigDecimal amount
    ) {
        return InvoiceItem.builder()
            .description(description)
            .itemType(InvoiceItemType.DISCOUNT)
            .quantity(1)
            .unitPrice(amount.negate())
            .amount(amount.negate())
            .build();
    }
}
