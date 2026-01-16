package com.screenleads.backend.app.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para estadísticas de un Customer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatsDTO {
    
    private Long customerId;
    
    // Métricas básicas
    private Integer totalRedemptions;
    private Integer uniquePromotionsRedeemed;
    private BigDecimal lifetimeValue;
    
    // Engagement
    private Integer engagementScore;
    private Instant firstInteractionAt;
    private Instant lastInteractionAt;
    private Long daysSinceLastInteraction;
    
    // Tasa de conversión
    private Integer totalViews; // Impresiones únicas
    private Double conversionRate; // redemptions / views
    
    // Actividad reciente
    private Integer redemptionsLast7Days;
    private Integer redemptionsLast30Days;
    private Integer redemptionsLast90Days;
    
    // Valor promedio
    private BigDecimal avgRedemptionValue;
    
    // Frecuencia
    private Double avgDaysBetweenRedemptions;
    
    // Promociones favoritas (top 3)
    private String topPromotions; // JSON array con nombres
    
    // Dispositivos usados (top 3)
    private String topDevices; // JSON array con nombres
}
