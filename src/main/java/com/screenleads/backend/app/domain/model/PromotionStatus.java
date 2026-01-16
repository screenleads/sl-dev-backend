package com.screenleads.backend.app.domain.model;

/**
 * Estado de una promoción
 */
public enum PromotionStatus {
    /**
     * Borrador - no visible para usuarios
     */
    DRAFT,
    
    /**
     * Programada - visible pero aún no ha comenzado
     */
    SCHEDULED,
    
    /**
     * Activa - canjeables por usuarios
     */
    ACTIVE,
    
    /**
     * Pausada manualmente
     */
    PAUSED,
    
    /**
     * Completada - alcanzó el límite de canjes
     */
    COMPLETED,
    
    /**
     * Cancelada manualmente
     */
    CANCELLED,
    
    /**
     * Presupuesto agotado - pausada automáticamente
     */
    BUDGET_EXHAUSTED
}
