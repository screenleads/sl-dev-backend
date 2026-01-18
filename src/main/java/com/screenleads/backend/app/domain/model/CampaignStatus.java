package com.screenleads.backend.app.domain.model;

/**
 * Estado del ciclo de vida de una campaña de marketing
 */
public enum CampaignStatus {
    /**
     * Campaña creada pero no programada ni activa
     */
    DRAFT,
    
    /**
     * Campaña programada para ejecución futura
     */
    SCHEDULED,
    
    /**
     * Campaña en proceso de ejecución (enviando notificaciones)
     */
    RUNNING,
    
    /**
     * Campaña completada exitosamente
     */
    COMPLETED,
    
    /**
     * Campaña pausada temporalmente
     */
    PAUSED,
    
    /**
     * Campaña cancelada antes de completarse
     */
    CANCELLED,
    
    /**
     * Campaña falló durante la ejecución
     */
    FAILED
}
