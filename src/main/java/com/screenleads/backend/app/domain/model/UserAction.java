package com.screenleads.backend.app.domain.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;

/**
 * Registro de acciones realizadas por un usuario en el sistema.
 * Permite tracking completo del comportamiento y análisis de patrones.
 */
@Entity
@Table(
    name = "user_action",
    indexes = {
        @Index(name = "ix_useraction_customer", columnList = "customer_id"),
        @Index(name = "ix_useraction_type", columnList = "action_type"),
        @Index(name = "ix_useraction_timestamp", columnList = "timestamp"),
        @Index(name = "ix_useraction_device", columnList = "device_id"),
        @Index(name = "ix_useraction_session", columnList = "session_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_useraction_customer"))
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id",
        foreignKey = @ForeignKey(name = "fk_useraction_device"))
    private Device device;

    @Column(nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private UserActionType actionType;

    // === Entidad relacionada (polimórfica) ===
    @Column(name = "entity_type", length = 50)
    private String entityType; // Promotion, Device, Advice, etc.

    @Column(name = "entity_id")
    private Long entityId;

    // === Datos de contexto ===
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Lob
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    // === Detalles de la acción ===
    @Lob
    @Column(columnDefinition = "TEXT")
    private String details; // JSON con información adicional
    
    /* Ejemplos de details JSON:
    {
      "previousPage": "/promotions",
      "formData": {"step": 2},
      "duration": 45,
      "interactionCount": 3,
      "scrollDepth": 0.75,
      "errorCode": "INVALID_EMAIL"
    }
    */

    // === Geolocalización (opcional) ===
    @Column(precision = 10, scale = 7)
    private Double latitude;
    
    @Column(precision = 10, scale = 7)
    private Double longitude;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String country;
    
    // === Métodos helper ===
    
    /**
     * Crea una acción de visualización de promoción
     */
    public static UserAction viewPromotion(Customer customer, Promotion promotion, Device device, String sessionId) {
        return UserAction.builder()
            .customer(customer)
            .device(device)
            .timestamp(Instant.now())
            .actionType(UserActionType.VIEW_PROMOTION)
            .entityType("Promotion")
            .entityId(promotion.getId())
            .sessionId(sessionId)
            .build();
    }
    
    /**
     * Crea una acción de canje de promoción
     */
    public static UserAction redeemPromotion(Customer customer, PromotionRedemption redemption, Device device, String sessionId) {
        return UserAction.builder()
            .customer(customer)
            .device(device)
            .timestamp(Instant.now())
            .actionType(UserActionType.REDEEM_PROMOTION)
            .entityType("PromotionRedemption")
            .entityId(redemption.getId())
            .sessionId(sessionId)
            .build();
    }
}
