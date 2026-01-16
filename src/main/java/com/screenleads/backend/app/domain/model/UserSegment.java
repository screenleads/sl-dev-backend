package com.screenleads.backend.app.domain.model;

/**
 * Segmentación de usuarios para remarketing
 */
public enum UserSegment {
    /**
     * Usuario sin interacción reciente o con baja engagement
     */
    COLD,
    
    /**
     * Usuario con interacción ocasional
     */
    WARM,
    
    /**
     * Usuario con interacción frecuente
     */
    HOT,
    
    /**
     * Usuario de alto valor (múltiples canjes, alto engagement)
     */
    VIP
}
