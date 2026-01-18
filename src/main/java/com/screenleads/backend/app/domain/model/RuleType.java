package com.screenleads.backend.app.domain.model;

/**
 * Tipo de regla de geofencing para promociones
 */
public enum RuleType {
    /**
     * Mostrar promoción solo dentro de la zona
     */
    SHOW_INSIDE,
    
    /**
     * Ocultar promoción fuera de la zona
     */
    HIDE_OUTSIDE,
    
    /**
     * Mostrar con prioridad alta dentro de la zona
     */
    PRIORITIZE_INSIDE,
    
    /**
     * Bloquear completamente fuera de la zona
     */
    BLOCK_OUTSIDE
}
