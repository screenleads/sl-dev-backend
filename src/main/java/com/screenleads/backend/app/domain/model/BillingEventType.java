package com.screenleads.backend.app.domain.model;

/**
 * Tipos de eventos de facturación para auditoría completa
 */
public enum BillingEventType {
    // === Eventos de leads ===
    /**
     * Lead registrado en el sistema
     */
    LEAD_REGISTERED,
    
    /**
     * Lead marcado como facturable
     */
    LEAD_BILLED,
    
    // === Eventos de Stripe ===
    /**
     * Uso reportado a Stripe
     */
    USAGE_REPORTED,
    
    /**
     * Factura creada en Stripe
     */
    INVOICE_CREATED,
    
    /**
     * Factura finalizada (cerrada para edición)
     */
    INVOICE_FINALIZED,
    
    /**
     * Factura enviada al cliente
     */
    INVOICE_SENT,
    
    /**
     * Pago exitoso
     */
    PAYMENT_SUCCEEDED,
    
    /**
     * Pago fallido
     */
    PAYMENT_FAILED,
    
    /**
     * Reembolso emitido
     */
    REFUND_ISSUED,
    
    // === Eventos de suscripción ===
    /**
     * Suscripción creada
     */
    SUBSCRIPTION_CREATED,
    
    /**
     * Suscripción actualizada
     */
    SUBSCRIPTION_UPDATED,
    
    /**
     * Suscripción cancelada
     */
    SUBSCRIPTION_CANCELLED,
    
    // === Eventos de cliente ===
    /**
     * Cliente creado en Stripe
     */
    CUSTOMER_CREATED,
    
    /**
     * Cliente actualizado en Stripe
     */
    CUSTOMER_UPDATED,
    
    // === Otros ===
    /**
     * Ajuste manual
     */
    MANUAL_ADJUSTMENT,
    
    /**
     * Error de sincronización
     */
    SYNC_ERROR
}
