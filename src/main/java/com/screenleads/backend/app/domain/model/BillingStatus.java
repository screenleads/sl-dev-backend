package com.screenleads.backend.app.domain.model;

/**
 * Estado de facturación de una empresa
 */
public enum BillingStatus {
    /**
     * Pendiente de configurar Stripe
     */
    INCOMPLETE,
    
    /**
     * Activo y al corriente de pagos
     */
    ACTIVE,
    
    /**
     * Pago atrasado
     */
    PAST_DUE,
    
    /**
     * Suspendido por impago
     */
    SUSPENDED,
    
    /**
     * Cancelado
     */
    CANCELLED
}
