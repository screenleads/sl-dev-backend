package com.screenleads.backend.app.domain.model;

/**
 * Tipos de acciones que puede realizar un usuario en el sistema
 */
public enum UserActionType {
    // === Visualización ===
    /**
     * Visualizó una promoción
     */
    VIEW_PROMOTION,
    
    /**
     * Visualizó un anuncio (Advice)
     */
    VIEW_ADVICE,
    
    /**
     * Visualizó términos y condiciones
     */
    VIEW_TERMS,
    
    /**
     * Visualizó política de privacidad
     */
    VIEW_PRIVACY,
    
    // === Interacción ===
    /**
     * Hizo clic en una promoción
     */
    CLICK_PROMOTION,
    
    /**
     * Hizo clic en un anuncio
     */
    CLICK_ADVICE,
    
    /**
     * Abrió el formulario de registro
     */
    OPEN_FORM,
    
    /**
     * Cerró el formulario de registro
     */
    CLOSE_FORM,
    
    // === Registro ===
    /**
     * Inició el proceso de registro
     */
    START_REGISTRATION,
    
    /**
     * Completó el registro exitosamente
     */
    COMPLETE_REGISTRATION,
    
    /**
     * Abandonó el registro sin completar
     */
    ABANDON_REGISTRATION,
    
    // === Verificación ===
    /**
     * Se envió email de verificación
     */
    EMAIL_VERIFICATION_SENT,
    
    /**
     * Email verificado exitosamente
     */
    EMAIL_VERIFIED,
    
    /**
     * Se envió código de verificación por SMS
     */
    PHONE_VERIFICATION_SENT,
    
    /**
     * Teléfono verificado exitosamente
     */
    PHONE_VERIFIED,
    
    // === Canjes ===
    /**
     * Canjeó una promoción
     */
    REDEEM_PROMOTION,
    
    /**
     * Visualizó el cupón generado
     */
    VIEW_COUPON,
    
    /**
     * Usó el cupón en el negocio externo
     */
    USE_COUPON,
    
    // === Autenticación ===
    /**
     * Inició sesión
     */
    LOGIN,
    
    /**
     * Cerró sesión
     */
    LOGOUT,
    
    /**
     * Añadió un nuevo método de autenticación
     */
    AUTH_METHOD_ADDED,
    
    // === Remarketing ===
    /**
     * Abrió un email de marketing
     */
    EMAIL_OPENED,
    
    /**
     * Hizo clic en un email de marketing
     */
    EMAIL_CLICKED,
    
    /**
     * Recibió un SMS
     */
    SMS_RECEIVED,
    
    /**
     * Recibió una notificación push
     */
    PUSH_RECEIVED,
    
    /**
     * Hizo clic en una notificación push
     */
    PUSH_CLICKED,
    
    // === Consentimientos ===
    /**
     * Aceptó consentimiento de marketing
     */
    MARKETING_CONSENT_ACCEPTED,
    
    /**
     * Revocó consentimiento de marketing
     */
    MARKETING_CONSENT_REVOKED,
    
    /**
     * Aceptó procesamiento de datos
     */
    DATA_PROCESSING_ACCEPTED,
    
    /**
     * Aceptó compartir datos con terceros
     */
    THIRD_PARTY_SHARING_ACCEPTED,
    
    // === Otros ===
    /**
     * Compartió contenido en redes sociales
     */
    SHARE,
    
    /**
     * Envió feedback
     */
    FEEDBACK,
    
    /**
     * Ocurrió un error
     */
    ERROR,
    
    /**
     * Acción personalizada
     */
    CUSTOM
}
