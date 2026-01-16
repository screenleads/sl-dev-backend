package com.screenleads.backend.app.domain.model;

/**
 * Estados posibles de un evento de facturación
 */
public enum BillingEventStatus {
    /**
     * Pendiente de procesar
     */
    PENDING,
    
    /**
     * Procesado exitosamente
     */
    SUCCESS,
    
    /**
     * Falló el procesamiento
     */
    FAILED,
    
    /**
     * Marcado para reintentar
     */
    RETRY,
    
    /**
     * Cancelado manualmente
     */
    CANCELLED
}
