package com.screenleads.backend.app.domain.model;

/**
 * Tipos de reglas de detección de fraude
 */
public enum FraudRuleType {
    /**
     * Detecta velocidad anormal de canjes (ej: 10 canjes en 1 hora)
     */
    VELOCITY,
    
    /**
     * Detecta múltiples canjes desde el mismo dispositivo
     */
    DUPLICATE_DEVICE,
    
    /**
     * Detecta IPs o dispositivos en lista negra
     */
    BLACKLIST,
    
    /**
     * Detecta anomalías de ubicación (cambios imposibles de ubicación)
     */
    LOCATION_ANOMALY,
    
    /**
     * Detecta patrones de comportamiento sospechosos
     */
    BEHAVIOR_PATTERN,
    
    /**
     * Regla personalizada definida por el usuario
     */
    CUSTOM
}
