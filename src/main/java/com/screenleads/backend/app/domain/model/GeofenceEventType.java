package com.screenleads.backend.app.domain.model;

/**
 * Tipo de evento de geofencing
 */
public enum GeofenceEventType {
    /**
     * Dispositivo entra en la zona
     */
    ENTER,

    /**
     * Dispositivo sale de la zona
     */
    EXIT,

    /**
     * Dispositivo permanece en la zona (dwell)
     */
    DWELL,

    /**
     * Actualización de posición dentro de la zona
     */
    UPDATE
}
