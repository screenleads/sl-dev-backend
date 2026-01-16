package com.screenleads.backend.app.domain.model;

/**
 * Estado de revisión antifraude de un canje
 */
public enum FraudStatus {
    /**
     * Pendiente de revisión
     */
    PENDING,
    
    /**
     * Limpio - no se detectó fraude
     */
    CLEAN,
    
    /**
     * Sospechoso - requiere revisión manual
     */
    SUSPICIOUS,
    
    /**
     * Fraude confirmado
     */
    FRAUD,
    
    /**
     * En revisión manual
     */
    MANUAL_REVIEW
}
