package com.screenleads.backend.app.domain.model;

/**
 * Método de verificación de un canje
 */
public enum VerificationMethod {
    /**
     * Link enviado por email
     */
    EMAIL_LINK,
    
    /**
     * Código SMS
     */
    SMS_CODE,
    
    /**
     * Verificación por WhatsApp
     */
    WHATSAPP,
    
    /**
     * Llamada telefónica
     */
    PHONE_CALL,
    
    /**
     * Verificación manual por operador
     */
    MANUAL,
    
    /**
     * Sin verificación
     */
    NONE
}
