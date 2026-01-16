package com.screenleads.backend.app.domain.model;

/**
 * Tipo de identificador requerido para canjear una promoción
 */
public enum RequiredIdentifier {
    /**
     * Solo email
     */
    EMAIL,
    
    /**
     * Solo teléfono
     */
    PHONE,
    
    /**
     * Email O teléfono (cualquiera de los dos)
     */
    EMAIL_OR_PHONE,
    
    /**
     * Email Y teléfono (ambos obligatorios)
     */
    EMAIL_AND_PHONE
}
