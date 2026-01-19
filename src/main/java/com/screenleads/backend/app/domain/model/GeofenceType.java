package com.screenleads.backend.app.domain.model;

/**
 * Tipo de geometría para una zona geográfica
 */
public enum GeofenceType {
    /**
     * Zona circular definida por centro (lat, lon) y radio en metros
     */
    CIRCLE,

    /**
     * Zona poligonal definida por array de coordenadas
     */
    POLYGON,

    /**
     * Zona rectangular definida por dos esquinas (bounding box)
     */
    RECTANGLE
}
