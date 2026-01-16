package com.screenleads.backend.app.domain.model;

/**
 * Métodos de autenticación/registro disponibles para los usuarios finales
 */
public enum AuthMethod {
    /**
     * Registro con email y contraseña (si aplica)
     */
    EMAIL,
    
    /**
     * Registro con número de teléfono (SMS)
     */
    PHONE,
    
    /**
     * Autenticación vía Instagram
     */
    INSTAGRAM,
    
    /**
     * Autenticación vía Facebook
     */
    FACEBOOK,
    
    /**
     * Autenticación vía WhatsApp
     */
    WHATSAPP,
    
    /**
     * Autenticación vía Google
     */
    GOOGLE,
    
    /**
     * Autenticación vía Apple
     */
    APPLE,
    
    /**
     * Autenticación vía Twitter/X
     */
    TWITTER,
    
    /**
     * Autenticación vía LinkedIn
     */
    LINKEDIN,
    
    /**
     * Otro método de autenticación
     */
    OTHER
}
