package com.screenleads.backend.app.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

/**
 * Representa al cliente/consumidor final que canjea promociones en los dispositivos.
 * Esta es la nueva entidad que reemplaza las antiguas Customer y Client.
 * Los usuarios de la plataforma (dashboard/app) se modelan con la entidad User.
 */
@Entity
@Table(name = "customer",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_customer_phone", columnNames = "phone")
    },
    indexes = {
        @Index(name = "ix_customer_email", columnList = "email"),
        @Index(name = "ix_customer_phone", columnList = "phone"),
        @Index(name = "ix_customer_created", columnList = "created_at"),
        @Index(name = "ix_customer_segment", columnList = "segment")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador principal (email o teléfono)
    @Column(name = "identifier", nullable = false, length = 320)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 20)
    private LeadIdentifierType identifierType;
    
    // === Datos personales ===
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Email
    @Column(unique = true, length = 320)
    private String email;
    
    @Column(unique = true, length = 50)
    private String phone; // Formato E.164: +34612345678
    
    private LocalDate birthDate;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;
    
    // === Ubicación ===
    @Column(length = 100)
    private String city;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(length = 100)
    private String country;
    
    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "es"; // es, en, fr...
    
    // === Métodos de registro/autenticación ===
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "customer_auth_method",
        joinColumns = @JoinColumn(name = "customer_id")
    )
    @Column(name = "auth_method")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<AuthMethod> authMethods = new HashSet<>();
    
    // IDs de redes sociales (JSON)
    @Lob
    @Column(name = "social_profiles", columnDefinition = "TEXT")
    private String socialProfiles; 
    /* Ejemplo JSON:
    {
      "instagram": "@usuario",
      "facebook": "123456789",
      "whatsapp": "+34612345678",
      "google": "user@gmail.com"
    }
    */
    
    // === Consentimientos GDPR ===
    @Builder.Default
    private Boolean marketingOptIn = false;
    
    private Instant marketingOptInAt;
    
    @Builder.Default
    private Boolean dataProcessingConsent = false;
    
    private Instant dataProcessingConsentAt;
    
    @Builder.Default
    private Boolean thirdPartyDataSharing = false;
    
    private Instant thirdPartyDataSharingAt;
    
    // === Estado de verificación ===
    @Builder.Default
    private Boolean emailVerified = false;
    
    private Instant emailVerifiedAt;
    
    @Builder.Default
    private Boolean phoneVerified = false;
    
    private Instant phoneVerifiedAt;
    
    // === Segmentación y scoring ===
    @Lob
    @Column(columnDefinition = "TEXT")
    private String tags; // JSON: ["vip", "frequent", "high-value"]
    
    private Integer engagementScore; // 0-100
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserSegment segment; // COLD, WARM, HOT, VIP
    
    // === Métricas agregadas ===
    @Builder.Default
    private Integer totalRedemptions = 0;
    
    @Builder.Default
    private Integer uniquePromotionsRedeemed = 0;
    
    @Column(name = "lifetime_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lifetimeValue = BigDecimal.ZERO;
    
    private Instant firstInteractionAt;
    private Instant lastInteractionAt;
    
    // === Relaciones ===
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PromotionRedemption> redemptions = new java.util.ArrayList<>();
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserAction> actions = new java.util.ArrayList<>();
    
    // === Métodos helper ===
    
    /**
     * Añade un método de autenticación al usuario
     */
    public void addAuthMethod(AuthMethod method) {
        if (this.authMethods == null) {
            this.authMethods = new HashSet<>();
        }
        this.authMethods.add(method);
    }
    
    /**
     * Incrementa el contador de canjes y actualiza timestamps
     */
    public void incrementRedemptions() {
        this.totalRedemptions++;
        this.lastInteractionAt = Instant.now();
        if (this.firstInteractionAt == null) {
            this.firstInteractionAt = Instant.now();
        }
    }
    
    /**
     * Actualiza el timestamp de última interacción
     */
    public void updateLastInteraction() {
        this.lastInteractionAt = Instant.now();
        if (this.firstInteractionAt == null) {
            this.firstInteractionAt = Instant.now();
        }
    }
    
    /**
     * Retorna el nombre completo del usuario
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
    
    /**
     * Retorna el identificador principal (email o teléfono)
     */
    public String getPrimaryIdentifier() {
        if (identifier != null) {
            return identifier;
        }
        if (email != null) {
            return email;
        }
        if (phone != null) {
            return phone;
        }
        return "user-" + id;
    }
    
    /**
     * Establece el identificador principal (email o teléfono)
     */
    public void setPrimaryIdentifier() {
        if (email != null) {
            this.identifier = email;
            this.identifierType = LeadIdentifierType.EMAIL;
        } else if (phone != null) {
            this.identifier = phone;
            this.identifierType = LeadIdentifierType.PHONE;
        } else {
            this.identifier = "user-" + id;
            this.identifierType = LeadIdentifierType.OTHER;
        }
    }
    /**
     * Verifica si el usuario ha dado consentimiento para marketing
     */
    public boolean hasMarketingConsent() {
        return Boolean.TRUE.equals(marketingOptIn) && marketingOptInAt != null;
    }
    
    /**
     * Verifica si el usuario está verificado (email o teléfono)
     */
    public boolean isVerified() {
        return Boolean.TRUE.equals(emailVerified) || Boolean.TRUE.equals(phoneVerified);
    }
}
