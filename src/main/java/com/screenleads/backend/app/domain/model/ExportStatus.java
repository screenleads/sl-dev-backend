package com.screenleads.backend.app.domain.model;

/**
 * Estados posibles de una exportación de datos
 */
public enum ExportStatus {
    /**
     * Pendiente de iniciar procesamiento
     */
    PENDING,
    
    /**
     * En proceso de generación
     */
    PROCESSING,
    
    /**
     * Completada exitosamente y lista para descargar
     */
    COMPLETED,
    
    /**
     * Falló durante el procesamiento
     */
    FAILED,
    
    /**
     * Expirada - el archivo ya no está disponible
     */
    EXPIRED
}
