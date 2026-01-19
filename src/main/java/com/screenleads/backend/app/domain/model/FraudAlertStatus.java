package com.screenleads.backend.app.domain.model;

/**
 * Estado de una alerta de fraude
 */
public enum FraudAlertStatus {
    /**
     * Alerta pendiente de revisión
     */
    PENDING,

    /**
     * Alerta bajo investigación
     */
    INVESTIGATING,

    /**
     * Alerta confirmada como fraude
     */
    CONFIRMED,

    /**
     * Alerta marcada como falso positivo
     */
    FALSE_POSITIVE,

    /**
     * Alerta resuelta
     */
    RESOLVED
}
