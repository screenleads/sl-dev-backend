package com.screenleads.backend.app.domain.model;

/**
 * Estados operativos posibles de un dispositivo
 */
public enum DeviceStatus {
    /**
     * Dispositivo activo y operativo
     */
    ACTIVE,
    
    /**
     * Dispositivo inactivo temporalmente
     */
    INACTIVE,
    
    /**
     * En mantenimiento
     */
    MAINTENANCE,
    
    /**
     * Desconectado/sin conexión
     */
    OFFLINE,
    
    /**
     * Dado de baja permanentemente
     */
    DECOMMISSIONED
}
