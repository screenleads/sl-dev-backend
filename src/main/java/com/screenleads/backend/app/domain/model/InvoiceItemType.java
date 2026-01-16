package com.screenleads.backend.app.domain.model;

/**
 * Tipos de líneas que puede tener una factura
 */
public enum InvoiceItemType {
    /**
     * Cuota base del plan
     */
    BASE_FEE,
    
    /**
     * Leads facturados por uso
     */
    LEADS,
    
    /**
     * Exceso sobre la cuota incluida
     */
    OVERAGE,
    
    /**
     * Descuento aplicado
     */
    DISCOUNT,
    
    /**
     * Ajuste manual (corrección)
     */
    ADJUSTMENT,
    
    /**
     * Devolución/reembolso
     */
    REFUND
}
