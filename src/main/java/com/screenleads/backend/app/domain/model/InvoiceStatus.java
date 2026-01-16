package com.screenleads.backend.app.domain.model;

/**
 * Estados posibles de una factura
 */
public enum InvoiceStatus {
    /**
     * Borrador - aún se puede editar
     */
    DRAFT,
    
    /**
     * Finalizada - no se puede editar, lista para enviar
     */
    FINALIZED,
    
    /**
     * Enviada al cliente
     */
    SENT,
    
    /**
     * Pagada
     */
    PAID,
    
    /**
     * Vencida - no pagada en plazo
     */
    PAST_DUE,
    
    /**
     * Anulada
     */
    VOID,
    
    /**
     * Reembolsada
     */
    REFUNDED
}
