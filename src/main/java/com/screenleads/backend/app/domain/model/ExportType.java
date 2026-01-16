package com.screenleads.backend.app.domain.model;

/**
 * Tipos de exportaciones de datos disponibles
 */
public enum ExportType {
    /**
     * Exportación de clientes/consumidores finales
     */
    CUSTOMERS,
    
    /**
     * Exportación de canjes de promociones
     */
    REDEMPTIONS,
    
    /**
     * Exportación completa (clientes + canjes + acciones)
     */
    FULL_DATASET,
    
    /**
     * Solicitud de datos GDPR (derecho de acceso)
     */
    GDPR_REQUEST,
    
    /**
     * Exportación personalizada con campos específicos
     */
    CUSTOM
}
