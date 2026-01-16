package com.screenleads.backend.app.web.dto;

import java.time.Instant;

import com.screenleads.backend.app.domain.model.UserSegment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para criterios de búsqueda de Customers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSearchCriteria {
    
    // Búsqueda por texto (email, phone, nombre)
    private String searchTerm;
    
    // Filtros específicos
    private UserSegment segment;
    private String city;
    private String country;
    
    // Estado de verificación
    private Boolean emailVerified;
    private Boolean phoneVerified;
    
    // Consentimientos
    private Boolean marketingOptIn;
    
    // Tags (JSON array contains)
    private String tag;
    
    // Rango de fechas
    private Instant createdFrom;
    private Instant createdTo;
    private Instant lastInteractionFrom;
    private Instant lastInteractionTo;
    
    // Métricas
    private Integer minRedemptions;
    private Integer maxRedemptions;
    private Integer minEngagementScore;
    private Integer maxEngagementScore;
    
    // Ordenamiento
    @Builder.Default
    private String sortBy = "createdAt"; // createdAt, lastInteractionAt, totalRedemptions, lifetimeValue
    
    @Builder.Default
    private String sortOrder = "DESC"; // ASC, DESC
}
