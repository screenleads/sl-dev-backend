package com.screenleads.backend.app.domain.model;

/**
 * Estado de facturación de un canje
 */
public enum RedemptionBillingStatus {
    /**
     * Pendiente de facturar
     */
    PENDING,
    
    /**
     * Facturado correctamente
     */
    BILLED,
    
    /**
     * Error en la facturación
     */
    FAILED,
    
    /**
     * Reembolsado
     */
    REFUNDED,
    
    /**
     * Excluido de facturación (test, fraude, etc.)
     */
    EXCLUDED
}
