package com.screenleads.backend.app.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

/**
 * Representa una factura mensual generada para una empresa.
 * Incluye todos los leads facturados en el periodo y su desglose.
 */
@Entity
@Table(name = "invoice",
    indexes = {
        @Index(name = "ix_invoice_billing", columnList = "company_billing_id"),
        @Index(name = "ix_invoice_period", columnList = "period_start, period_end"),
        @Index(name = "ix_invoice_status", columnList = "status"),
        @Index(name = "ix_invoice_number", columnList = "invoice_number", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_billing_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_invoice_billing"))
    private CompanyBilling companyBilling;

    // === Identificación ===
    @Column(name = "invoice_number", unique = true, length = 50, nullable = false)
    private String invoiceNumber; // INV-2026-001234

    @Column(name = "stripe_invoice_id", length = 64)
    private String stripeInvoiceId;

    // === Periodo facturado ===
    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    // === Fechas ===
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    // === Importes ===
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO; // 21.00 para IVA en España

    @Column(name = "tax_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "EUR";

    // === Estado ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    // === Datos fiscales (snapshot en momento de emisión) ===
    @Column(name = "company_legal_name", length = 255)
    private String companyLegalName;

    @Column(name = "company_tax_id", length = 50)
    private String companyTaxId;

    @Lob
    @Column(name = "company_address", columnDefinition = "TEXT")
    private String companyAddress;

    // === URLs ===
    @Column(name = "pdf_url", length = 2048)
    private String pdfUrl; // URL del PDF generado

    @Column(name = "stripe_hosted_url", length = 2048)
    private String stripeHostedUrl; // URL de Stripe

    // === Notas ===
    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    // === Relaciones ===
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    // === Métodos helper ===
    
    /**
     * Calcula los totales de la factura a partir de sus líneas
     */
    public void calculateTotals() {
        this.subtotal = items.stream()
            .map(InvoiceItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.taxAmount = subtotal.multiply(taxRate).divide(
            BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        this.total = subtotal.add(taxAmount).subtract(discountAmount);
    }
    
    /**
     * Añade una línea a la factura y recalcula totales
     */
    public void addItem(InvoiceItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        item.setInvoice(this);
        this.items.add(item);
        calculateTotals();
    }
    
    /**
     * Marca la factura como pagada
     */
    public void markAsPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidAt = Instant.now();
    }
    
    /**
     * Finaliza la factura (no se puede editar más)
     */
    public void finalize() {
        if (this.status == InvoiceStatus.DRAFT) {
            this.status = InvoiceStatus.FINALIZED;
            calculateTotals();
        }
    }
    
    /**
     * Verifica si la factura está vencida
     */
    public boolean isOverdue() {
        if (status == InvoiceStatus.PAID || status == InvoiceStatus.VOID) {
            return false;
        }
        return LocalDate.now().isAfter(dueDate);
    }
    
    /**
     * Obtiene el número de días de retraso
     */
    public long getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }
}
