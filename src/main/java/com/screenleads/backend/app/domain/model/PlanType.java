package com.screenleads.backend.app.domain.model;

/**
 * Tipo de plan de facturación
 */
public enum PlanType {
    /**
     * Plan gratuito con límites
     */
    FREE,
    
    /**
     * Pago por uso (metered billing)
     */
    METERED,
    
    /**
     * Tarifa plana mensual
     */
    FLAT_RATE,
    
    /**
     * Plan personalizado negociado
     */
    CUSTOM
}
