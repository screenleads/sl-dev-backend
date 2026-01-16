package com.screenleads.backend.app.domain.model;

/**
 * Tipo de límite de canjes para una promoción
 */
public enum RedemptionLimitType {
    /**
     * Sin límite - el usuario puede canjear infinitas veces
     */
    UNLIMITED,
    
    /**
     * Un canje por usuario (lifetime) - el usuario solo puede canjear una vez
     */
    ONE_PER_USER,
    
    /**
     * Con intervalo de tiempo entre canjes - el usuario puede canjear múltiples veces 
     * pero debe esperar X horas entre cada canje
     */
    TIME_LIMITED_PER_USER,
    
    /**
     * Un canje al día por usuario
     */
    DAILY_PER_USER,
    
    /**
     * Reglas personalizadas (lógica custom en el servicio)
     */
    CUSTOM
}
