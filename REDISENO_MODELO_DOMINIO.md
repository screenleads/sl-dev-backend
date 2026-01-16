# Rediseño Completo del Modelo de Dominio - ScreenLeads Backend

**Fecha**: 13 de enero de 2026  
**Objetivo**: Rediseñar las entidades de usuarios, promociones, leads y facturación para un modelo coherente y escalable

---

## � DOCUMENTACIÓN RELACIONADA

**Actualización 14 de enero de 2026:**

Este documento es la especificación original del rediseño. Se han creado documentos adicionales para implementación:

1. **[DIAGRAMA_ENTIDADES_COMPLETO.md](../../DIAGRAMA_ENTIDADES_COMPLETO.md)** ⭐
   - Diagramas Mermaid interactivos de todas las entidades
   - 11 diagramas: 1 principal + 8 modulares + 2 de flujo
   - Matriz completa de dependencias
   - Métricas del sistema

2. **[DASHBOARD_ADAPTACION_REDISENO_2026.md](../../sl-dev-dashboard/DASHBOARD_ADAPTACION_REDISENO_2026.md)** ⭐
   - Plan completo de adaptación del dashboard Angular
   - Análisis de 16 features existentes
   - Especificación de 6 features nuevas
   - Plan de implementación en 5 fases (17 días)

3. **[GUIA_IMPLEMENTACION_FEATURES.md](../../sl-dev-dashboard/GUIA_IMPLEMENTACION_FEATURES.md)** ⭐
   - Código de ejemplo para todas las features nuevas
   - Modelos TypeScript completos
   - Servicios HTTP con todos los endpoints
   - Estructura estándar de carpetas

4. **[RESUMEN_TRABAJO_COMPLETADO.md](../../RESUMEN_TRABAJO_COMPLETADO.md)** ⭐
   - Resumen ejecutivo de todo el trabajo
   - Estadísticas y métricas
   - Próximos pasos recomendados
   - Checklist de implementación

---

## �📊 DIAGRAMA DEL NUEVO MODELO

```
┌─────────────┐
│   Company   │ 1───────* ┌──────────────────┐
└─────────────┘           │  CompanyBilling  │
      │ 1                 └──────────────────┘
      │                            │ 1
      │ *                          │ *
┌─────────────┐           ┌─────────────┐
│   Device    │           │   Invoice   │
└─────────────┘           └─────────────┘
      │ *                          │ 1
      │                            │ *
      │ * ┌──────────┐     ┌──────────────┐
      ├───│  Advice  │     │ InvoiceItem  │
      │   └──────────┘     └──────────────┘
      │        │ 0..1
      │        │
      │   ┌────────────┐
      │   │ Promotion  │ 1
      │   └────────────┘ │
      │                  │ *
      │          ┌────────────────────┐
      │          │ PromotionRedemption│
      │          └────────────────────┘
      │                  │ *
      │                  │ 1
      │          ┌──────────┐
      └──────────│ Customer │
                 └──────────┘
                      │ 1
                      │ *
                 ┌─────────────┐
                 │ UserAction  │
                 └─────────────┘
```

**Nota importante sobre nomenclatura**:
- `User` = Usuarios de la plataforma (dashboard/app de gestión)
- `Customer` = Clientes/consumidores finales que canjean promociones

---

## 🔄 CAMBIOS PRINCIPALES

### ❌ ELIMINAR (Completado ✅)
- `Customer` (antigua - confusa, reemplazada por nueva Customer)
- `Client` (duplicado, reemplazado por Customer)
- `PromotionLead` (reemplazado por PromotionRedemption)

### ✅ CREAR
- `Customer` (nueva) - Cliente/consumidor final que canjea promociones
- `PromotionRedemption` - Canje de una promoción
- `UserAction` - Historial de acciones del usuario
- `CompanyBilling` - Datos de facturación por empresa
- `Invoice` - Facturas mensuales
- `InvoiceItem` - Líneas de factura (leads por promoción)
- `BillingEvent` - Eventos de facturación (auditoría)
- `DataExport` - Exportaciones de datos para remarketing
- `DeviceLocation` - Ubicación detallada de dispositivos

### 🔧 REFACTORIZAR
- `Promotion` - Ampliar con nuevos campos y reglas
- `Device` - Añadir geolocalización
- `Company` - Mejorar campos de facturación

---

## 📝 ENTIDADES DETALLADAS

### 1. **Customer** (Cliente/Consumidor Final)

**Propósito**: Representa al cliente/consumidor final que canjea promociones en los dispositivos.
**Nota**: No confundir con `User` que representa usuarios de la plataforma (dashboard).

**Características**:
- Perfil unificado con múltiples métodos de autenticación
- Historial completo de canjes
- Datos personales para remarketing
- Consentimientos GDPR

```java
@Entity
@Table(name = "customer",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_customer_phone", columnNames = "phone")
    },
    indexes = {
        @Index(name = "ix_customer_email", columnList = "email"),
        @Index(name = "ix_customer_phone", columnList = "phone"),
        @Index(name = "ix_customer_created", columnList = "created_at")
    })
public class Customer extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
    private String preferredLanguage; // es, en, fr...
    
    // === Métodos de registro/autenticación ===
    @ElementCollection
    @CollectionTable(
        name = "customer_auth_method",
        joinColumns = @JoinColumn(name = "customer_id")
    )
    @Column(name = "auth_method")
    @Enumerated(EnumType.STRING)
    private Set<AuthMethod> authMethods = new HashSet<>();
    
    // IDs de redes sociales (JSON)
    @Column(name = "social_profiles", columnDefinition = "TEXT")
    private String socialProfiles; 
    /* Ejemplo:
    {
      "instagram": "@usuario",
      "facebook": "123456789",
      "whatsapp": "+34612345678",
      "google": "user@gmail.com"
    }
    */
    
    // === Consentimientos GDPR ===
    private Boolean marketingOptIn = false;
    private Instant marketingOptInAt;
    
    private Boolean dataProcessingConsent = false;
    private Instant dataProcessingConsentAt;
    
    private Boolean thirdPartyDataSharing = false;
    private Instant thirdPartyDataSharingAt;
    
    // === Estado de verificación ===
    private Boolean emailVerified = false;
    private Instant emailVerifiedAt;
    
    private Boolean phoneVerified = false;
    private Instant phoneVerifiedAt;
    
    // === Segmentación y scoring ===
    @Column(columnDefinition = "TEXT")
    private String tags; // JSON: ["vip", "frequent", "high-value"]
    
    private Integer engagementScore; // 0-100
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserSegment segment; // COLD, WARM, HOT, VIP
    
    // === Métricas agregadas ===
    private Integer totalRedemptions = 0;
    private Integer uniquePromotionsRedeemed = 0;
    
    @Column(name = "lifetime_value", precision = 10, scale = 2)
    private BigDecimal lifetimeValue = BigDecimal.ZERO;
    
    private Instant firstInteractionAt;
    private Instant lastInteractionAt;
    
    // === Relaciones ===
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<PromotionRedemption> redemptions;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<UserAction> actions;
    
    // === Métodos helper ===
    public void addAuthMethod(AuthMethod method) {
        if (this.authMethods == null) {
            this.authMethods = new HashSet<>();
        }
        this.authMethods.add(method);
    }
    
    public void incrementRedemptions() {
        this.totalRedemptions++;
        this.lastInteractionAt = Instant.now();
        if (this.firstInteractionAt == null) {
            this.firstInteractionAt = Instant.now();
        }
    }
}

enum AuthMethod {
    EMAIL,
    PHONE,
    INSTAGRAM,
    FACEBOOK,
    WHATSAPP,
    GOOGLE,
    APPLE,
    TWITTER,
    LINKEDIN,
    OTHER
}

enum Gender {
    MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
}

enum UserSegment {
    COLD,      // Sin interacción reciente
    WARM,      // Interacción ocasional
    HOT,       // Interacción frecuente
    VIP        // Alto valor
}
```

---

### 2. **Promotion** (Refactorizada)

**Cambios**:
- Nuevas reglas de límites de canje
- Templates HTML personalizables
- Códigos de cupón externos
- Configuración de requisitos de identificación
- Control de presupuesto y límites
- Geo-targeting

```java
@Entity
@Table(
    name = "promotion",
    indexes = {
        @Index(name = "ix_promotion_company", columnList = "company_id"),
        @Index(name = "ix_promotion_status", columnList = "status"),
        @Index(name = "ix_promotion_dates", columnList = "start_at, end_at")
    }
)
public class Promotion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_promotion_company"))
    private Company company;

    // === Información básica ===
    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;
    
    @Column(name = "short_description", length = 255)
    private String shortDescription; // Para listados

    // === URLs ===
    @Column(name = "terms_url", length = 2048)
    private String termsUrl; // Condiciones legales
    
    @Column(name = "privacy_url", length = 2048)
    private String privacyUrl;
    
    @Column(name = "external_redemption_url", length = 2048)
    private String externalRedemptionUrl; // URL externa para canjear
    
    @Column(name = "landing_url", length = 2048)
    private String landingUrl; // Landing page de la promoción

    // === Templates ===
    @Lob
    @Column(name = "template_html", columnDefinition = "TEXT")
    private String templateHtml; // Template personalizado del formulario
    
    @Lob
    @Column(name = "email_template_html", columnDefinition = "TEXT")
    private String emailTemplateHtml; // Template de email de confirmación
    
    @Lob
    @Column(name = "success_message", columnDefinition = "TEXT")
    private String successMessage; // Mensaje tras canjear

    // === Cupón externo ===
    @Column(name = "external_coupon_code", length = 120)
    private String externalCouponCode; // Código para canjear en negocio externo
    
    @Column(name = "coupon_prefix", length = 20)
    private String couponPrefix; // Prefijo para cupones generados: "SCREEN2024-"

    // === Ventana temporal ===
    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;
    
    @Column(name = "timezone", length = 50)
    private String timezone; // Europe/Madrid

    // === Reglas de identificación ===
    @Enumerated(EnumType.STRING)
    @Column(name = "required_identifier", length = 30, nullable = false)
    private RequiredIdentifier requiredIdentifier = RequiredIdentifier.EMAIL;
    
    @Column(name = "require_phone")
    private Boolean requirePhone = false;
    
    @Column(name = "require_email")
    private Boolean requireEmail = true;
    
    @Column(name = "require_birthdate")
    private Boolean requireBirthdate = false;
    
    @Column(name = "require_full_name")
    private Boolean requireFullName = true;

    // === Reglas de límites de canje ===
    @Enumerated(EnumType.STRING)
    @Column(name = "redemption_limit_type", length = 30, nullable = false)
    private RedemptionLimitType redemptionLimitType = RedemptionLimitType.ONE_PER_USER;
    
    // Si redemptionLimitType = TIME_LIMITED_PER_USER
    @Column(name = "redemption_interval_hours")
    private Integer redemptionIntervalHours; // Horas entre canjes del mismo usuario
    
    // Límite global de canjes
    @Column(name = "max_total_redemptions")
    private Integer maxTotalRedemptions;
    
    @Column(name = "max_redemptions_per_day")
    private Integer maxRedemptionsPerDay;
    
    @Column(name = "max_redemptions_per_user")
    private Integer maxRedemptionsPerUser;

    // === Presupuesto y pricing ===
    @Column(name = "budget_total", precision = 10, scale = 2)
    private BigDecimal budgetTotal;
    
    @Column(name = "budget_spent", precision = 10, scale = 2)
    private BigDecimal budgetSpent = BigDecimal.ZERO;
    
    @Column(name = "price_per_lead", precision = 10, scale = 2)
    private BigDecimal pricePerLead; // Precio específico de esta promo (override)

    // === Geo-targeting ===
    @Column(name = "target_countries", columnDefinition = "TEXT")
    private String targetCountries; // JSON: ["ES", "PT", "FR"]
    
    @Column(name = "target_regions", columnDefinition = "TEXT")
    private String targetRegions; // JSON: ["Madrid", "Barcelona"]
    
    @Column(name = "target_cities", columnDefinition = "TEXT")
    private String targetCities; // JSON: ["Madrid", "Barcelona"]

    // === Targeting por dispositivo ===
    @Column(name = "target_device_types", columnDefinition = "TEXT")
    private String targetDeviceTypes; // JSON: ["TABLET", "KIOSK"]
    
    @Column(name = "target_device_ids", columnDefinition = "TEXT")
    private String targetDeviceIds; // JSON: [1, 5, 7] - devices específicos

    // === Prioridad y estado ===
    @Column(nullable = false)
    private Integer priority = 0; // Mayor prioridad = se muestra más
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionStatus status = PromotionStatus.DRAFT;
    
    private Boolean active = true;

    // === Imágenes ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_id", foreignKey = @ForeignKey(name = "fk_promotion_thumbnail"))
    private Media thumbnail;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_id", foreignKey = @ForeignKey(name = "fk_promotion_banner"))
    private Media banner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_id", foreignKey = @ForeignKey(name = "fk_promotion_background"))
    private Media background;

    // === Métricas (desnormalizadas) ===
    @Column(name = "redemption_count")
    private Integer redemptionCount = 0;
    
    @Column(name = "unique_users_count")
    private Integer uniqueUsersCount = 0;
    
    @Column(name = "impression_count")
    private Long impressionCount = 0L;
    
    @Column(name = "click_count")
    private Long clickCount = 0L;

    // === Relaciones ===
    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL)
    private Set<PromotionRedemption> redemptions;
    
    // === Métodos helper ===
    public boolean isActive() {
        if (!this.active || this.status != PromotionStatus.ACTIVE) {
            return false;
        }
        
        Instant now = Instant.now();
        if (startAt != null && now.isBefore(startAt)) {
            return false;
        }
        if (endAt != null && now.isAfter(endAt)) {
            return false;
        }
        
        if (maxTotalRedemptions != null && redemptionCount >= maxTotalRedemptions) {
            return false;
        }
        
        if (budgetTotal != null && budgetSpent.compareTo(budgetTotal) >= 0) {
            return false;
        }
        
        return true;
    }
    
    public void incrementRedemption(BigDecimal cost) {
        this.redemptionCount++;
        if (cost != null) {
            this.budgetSpent = this.budgetSpent.add(cost);
        }
    }
    
    public void incrementImpression() {
        this.impressionCount++;
    }
    
    public void incrementClick() {
        this.clickCount++;
    }
    
    public BigDecimal getConversionRate() {
        if (clickCount == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(redemptionCount)
            .divide(BigDecimal.valueOf(clickCount), 4, RoundingMode.HALF_UP);
    }
}

enum RequiredIdentifier {
    EMAIL,
    PHONE,
    EMAIL_OR_PHONE,
    EMAIL_AND_PHONE
}

enum RedemptionLimitType {
    UNLIMITED,              // Sin límite
    ONE_PER_USER,          // Un canje por usuario (lifetime)
    TIME_LIMITED_PER_USER, // Con intervalo de tiempo entre canjes
    DAILY_PER_USER,        // Un canje al día por usuario
    CUSTOM                 // Reglas personalizadas
}

enum PromotionStatus {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
    BUDGET_EXHAUSTED
}
```

---

### 3. **PromotionRedemption** (Reemplaza PromotionLead)

**Propósito**: Representa el canje de una promoción por un usuario final.

```java
@Entity
@Table(
    name = "promotion_redemption",
    indexes = {
        @Index(name = "ix_redemption_promotion", columnList = "promotion_id"),
        @Index(name = "ix_redemption_customer", columnList = "customer_id"),
        @Index(name = "ix_redemption_device", columnList = "device_id"),
        @Index(name = "ix_redemption_created", columnList = "created_at"),
        @Index(name = "ix_redemption_status", columnList = "status")
    }
)
public class PromotionRedemption extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Relaciones ===
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_redemption_promotion"))
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_redemption_customer"))
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_redemption_device"))
    private Device device; // En qué dispositivo se canjeó

    // === Cupón generado ===
    @Column(name = "coupon_code", unique = true, length = 64, nullable = false)
    private String couponCode; // Código único generado

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_status", length = 20, nullable = false)
    private CouponStatus couponStatus = CouponStatus.VALID;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt; // Cuándo se usó en el negocio externo

    // === Datos del canje ===
    @Enumerated(EnumType.STRING)
    @Column(name = "redemption_method", length = 30)
    private AuthMethod redemptionMethod; // Cómo se registró

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // IPv4 o IPv6

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    // === Validación y verificación ===
    private Boolean verified = false;
    private Instant verifiedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", length = 30)
    private VerificationMethod verificationMethod;

    // === Calidad y fraude ===
    @Column(name = "lead_score")
    private Integer leadScore; // 0-100

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_status", length = 30)
    private FraudStatus fraudStatus = FraudStatus.PENDING;

    @Column(name = "fraud_check_details", columnDefinition = "TEXT")
    private String fraudCheckDetails; // JSON con detalles

    // === Tracking de origen ===
    @Column(name = "utm_source", length = 100)
    private String utmSource;

    @Column(name = "utm_medium", length = 100)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 100)
    private String utmCampaign;

    @Column(name = "utm_content", length = 100)
    private String utmContent;

    @Column(name = "referral_code", length = 50)
    private String referralCode;

    // === Métricas de interacción ===
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds; // Tiempo que tardó en el formulario

    @Column(name = "form_interactions")
    private Integer formInteractions; // Cuántos campos tocó

    @Column(name = "attempts")
    private Integer attempts = 1; // Intentos hasta completar

    // === Estado de facturación ===
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_status", length = 30)
    private RedemptionBillingStatus billingStatus = RedemptionBillingStatus.PENDING;

    @Column(name = "billed_at")
    private Instant billedAt;

    @Column(name = "billing_amount", precision = 10, scale = 2)
    private BigDecimal billingAmount;

    // === Metadata adicional ===
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON con datos adicionales
    
    // === Métodos helper ===
    public boolean canBeRedeemed() {
        if (couponStatus != CouponStatus.VALID) {
            return false;
        }
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }
    
    public void markAsRedeemed() {
        this.couponStatus = CouponStatus.REDEEMED;
        this.redeemedAt = Instant.now();
    }
}

enum CouponStatus {
    NEW,        // Generado pero no validado
    VALID,      // Validado y listo para usar
    REDEEMED,   // Ya usado en el negocio
    EXPIRED,    // Caducado
    CANCELLED   // Cancelado manualmente
}

enum VerificationMethod {
    EMAIL_LINK,
    SMS_CODE,
    WHATSAPP,
    PHONE_CALL,
    MANUAL,
    NONE
}

enum FraudStatus {
    PENDING,
    CLEAN,
    SUSPICIOUS,
    FRAUD,
    MANUAL_REVIEW
}

enum RedemptionBillingStatus {
    PENDING,
    BILLED,
    FAILED,
    REFUNDED,
    EXCLUDED // No se factura (test, fraude, etc.)
}
```

---

### 4. **UserAction** (Historial de Acciones)

**Propósito**: Registrar todas las acciones que realiza un usuario en el sistema.

```java
@Entity
@Table(
    name = "user_action",
    indexes = {
        @Index(name = "ix_useraction_customer", columnList = "customer_id"),
        @Index(name = "ix_useraction_type", columnList = "action_type"),
        @Index(name = "ix_useraction_timestamp", columnList = "timestamp"),
        @Index(name = "ix_useraction_device", columnList = "device_id")
    }
)
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

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    // === Detalles de la acción ===
    @Column(columnDefinition = "TEXT")
    private String details; // JSON con información adicional
    
    /* Ejemplos de details:
    {
      "previousPage": "/promotions",
      "formData": {"step": 2},
      "duration": 45,
      "interactionCount": 3
    }
    */

    // === Geolocalización (opcional) ===
    private Double latitude;
    private Double longitude;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String country;
}

enum UserActionType {
    // Visualización
    VIEW_PROMOTION,
    VIEW_ADVICE,
    VIEW_TERMS,
    
    // Interacción
    CLICK_PROMOTION,
    CLICK_ADVICE,
    OPEN_FORM,
    CLOSE_FORM,
    
    // Registro
    START_REGISTRATION,
    COMPLETE_REGISTRATION,
    ABANDON_REGISTRATION,
    
    // Verificación
    EMAIL_VERIFICATION_SENT,
    EMAIL_VERIFIED,
    PHONE_VERIFICATION_SENT,
    PHONE_VERIFIED,
    
    // Canjes
    REDEEM_PROMOTION,
    VIEW_COUPON,
    USE_COUPON,
    
    // Autenticación
    LOGIN,
    LOGOUT,
    AUTH_METHOD_ADDED,
    
    // Remarketing
    EMAIL_OPENED,
    EMAIL_CLICKED,
    SMS_RECEIVED,
    PUSH_RECEIVED,
    
    // Otros
    SHARE,
    FEEDBACK,
    ERROR,
    CUSTOM
}
```

---

### 5. **CompanyBilling** (Datos de Facturación)

**Propósito**: Centralizar toda la información de facturación de una empresa.

```java
@Entity
@Table(name = "company_billing",
    indexes = {
        @Index(name = "ix_billing_company", columnList = "company_id", unique = true)
    })
public class CompanyBilling extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true,
        foreignKey = @ForeignKey(name = "fk_billing_company"))
    private Company company;

    // === Stripe ===
    @Column(name = "stripe_customer_id", length = 64)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 64)
    private String stripeSubscriptionId;

    @Column(name = "stripe_subscription_item_id", length = 64)
    private String stripeSubscriptionItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_status", length = 32, nullable = false)
    private BillingStatus billingStatus = BillingStatus.INCOMPLETE;

    // === Datos fiscales ===
    @Column(name = "legal_name", length = 255)
    private String legalName; // Razón social

    @Column(name = "tax_id", length = 50)
    private String taxId; // NIF/CIF/VAT

    @Column(name = "billing_email", length = 320)
    private String billingEmail;

    @Column(name = "billing_phone", length = 50)
    private String billingPhone;

    // === Dirección de facturación ===
    @Column(name = "billing_address", length = 255)
    private String billingAddress;

    @Column(name = "billing_address_2", length = 255)
    private String billingAddress2;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state", length = 100)
    private String billingState;

    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;

    @Column(name = "billing_country", length = 2)
    private String billingCountry; // ISO 3166-1 alpha-2

    // === Plan y límites ===
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", length = 30)
    private PlanType planType = PlanType.METERED;

    @Column(name = "base_price", precision = 10, scale = 2)
    private BigDecimal basePrice = BigDecimal.ZERO; // Precio base mensual

    @Column(name = "price_per_lead", precision = 10, scale = 2)
    private BigDecimal pricePerLead; // Precio por lead (puede ser custom)

    @Column(name = "monthly_lead_quota")
    private Integer monthlyLeadQuota; // Cuota incluida en plan base

    @Column(name = "max_devices")
    private Integer maxDevices;

    @Column(name = "max_promotions")
    private Integer maxPromotions;

    // === Uso actual ===
    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "current_period_leads")
    private Integer currentPeriodLeads = 0;

    @Column(name = "current_period_amount", precision = 10, scale = 2)
    private BigDecimal currentPeriodAmount = BigDecimal.ZERO;

    // === Histórico ===
    @Column(name = "total_leads_billed")
    private Long totalLeadsBilled = 0L;

    @Column(name = "total_amount_billed", precision = 12, scale = 2)
    private BigDecimal totalAmountBilled = BigDecimal.ZERO;

    @Column(name = "last_invoice_date")
    private Instant lastInvoiceDate;

    // === Estado ===
    private Boolean active = true;
    
    @Column(name = "suspended_at")
    private Instant suspendedAt;
    
    @Column(name = "suspension_reason", length = 255)
    private String suspensionReason;

    // === Métodos de pago ===
    @Column(name = "payment_method_type", length = 30)
    private String paymentMethodType; // card, sepa_debit, etc.

    @Column(name = "payment_method_last4", length = 4)
    private String paymentMethodLast4;

    // === Notificaciones ===
    @Column(name = "invoice_emails", columnDefinition = "TEXT")
    private String invoiceEmails; // JSON: ["billing@company.com", "cfo@company.com"]

    // === Relaciones ===
    @OneToMany(mappedBy = "companyBilling", cascade = CascadeType.ALL)
    private List<Invoice> invoices;

    @OneToMany(mappedBy = "companyBilling", cascade = CascadeType.ALL)
    private List<BillingEvent> events;
}

enum BillingStatus {
    INCOMPLETE,     // Pendiente de configurar Stripe
    ACTIVE,         // Activo y al corriente
    PAST_DUE,       // Pago atrasado
    SUSPENDED,      // Suspendido por impago
    CANCELLED       // Cancelado
}

enum PlanType {
    FREE,           // Plan gratuito (limitado)
    METERED,        // Pago por uso
    FLAT_RATE,      // Tarifa plana
    CUSTOM          // Plan personalizado
}
```

---

### 6. **Invoice** (Factura)

**Propósito**: Factura mensual generada para una empresa.

```java
@Entity
@Table(name = "invoice",
    indexes = {
        @Index(name = "ix_invoice_billing", columnList = "company_billing_id"),
        @Index(name = "ix_invoice_period", columnList = "period_start, period_end"),
        @Index(name = "ix_invoice_status", columnList = "status")
    })
public class Invoice extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_billing_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_invoice_billing"))
    private CompanyBilling companyBilling;

    // === Identificación ===
    @Column(name = "invoice_number", unique = true, length = 50, nullable = false)
    private String invoiceNumber; // INV-2026-001234

    @Column(name = "stripe_invoice_id", length = 64)
    private String stripeInvoiceId;

    // === Periodo facturado ===
    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    // === Fechas ===
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    // === Importes ===
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO; // 21.00 para IVA en España

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 3)
    private String currency = "EUR";

    // === Estado ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    // === Datos fiscales (snapshot en momento de emisión) ===
    @Column(name = "company_legal_name", length = 255)
    private String companyLegalName;

    @Column(name = "company_tax_id", length = 50)
    private String companyTaxId;

    @Column(name = "company_address", columnDefinition = "TEXT")
    private String companyAddress;

    // === URLs ===
    @Column(name = "pdf_url", length = 2048)
    private String pdfUrl; // URL del PDF generado

    @Column(name = "stripe_hosted_url", length = 2048)
    private String stripeHostedUrl; // URL de Stripe

    // === Notas ===
    @Column(columnDefinition = "TEXT")
    private String notes;

    // === Relaciones ===
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<InvoiceItem> items;

    // === Métodos helper ===
    public void calculateTotals() {
        this.subtotal = items.stream()
            .map(InvoiceItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.taxAmount = subtotal.multiply(taxRate).divide(
            BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        this.total = subtotal.add(taxAmount).subtract(discountAmount);
    }
}

enum InvoiceStatus {
    DRAFT,
    FINALIZED,
    SENT,
    PAID,
    PAST_DUE,
    VOID,
    REFUNDED
}
```

---

### 7. **InvoiceItem** (Línea de Factura)

```java
@Entity
@Table(name = "invoice_item",
    indexes = {
        @Index(name = "ix_invoiceitem_invoice", columnList = "invoice_id"),
        @Index(name = "ix_invoiceitem_promotion", columnList = "promotion_id")
    })
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_invoiceitem_invoice"))
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id",
        foreignKey = @ForeignKey(name = "fk_invoiceitem_promotion"))
    private Promotion promotion; // Si se desglosa por promoción

    // === Descripción ===
    @Column(nullable = false, length = 255)
    private String description; // "Leads generados - Promoción Verano 2026"

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private InvoiceItemType itemType;

    // === Cantidad y precio ===
    @Column(nullable = false)
    private Integer quantity; // Número de leads

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice; // Precio por lead

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount; // quantity * unitPrice

    // === Periodo ===
    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    // === Metadata ===
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON con detalles adicionales
}

enum InvoiceItemType {
    BASE_FEE,           // Cuota base
    LEADS,              // Leads facturados
    OVERAGE,            // Exceso sobre cuota
    DISCOUNT,           // Descuento
    ADJUSTMENT,         // Ajuste manual
    REFUND              // Devolución
}
```

---

### 8. **BillingEvent** (Auditoría de Facturación)

```java
@Entity
@Table(name = "billing_event",
    indexes = {
        @Index(name = "ix_billingevent_billing", columnList = "company_billing_id"),
        @Index(name = "ix_billingevent_redemption", columnList = "redemption_id"),
        @Index(name = "ix_billingevent_timestamp", columnList = "timestamp"),
        @Index(name = "ix_billingevent_type", columnList = "event_type")
    })
public class BillingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_billing_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_billingevent_billing"))
    private CompanyBilling companyBilling;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redemption_id",
        foreignKey = @ForeignKey(name = "fk_billingevent_redemption"))
    private PromotionRedemption redemption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id",
        foreignKey = @ForeignKey(name = "fk_billingevent_invoice"))
    private Invoice invoice;

    @Column(nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private BillingEventType eventType;

    // === Stripe ===
    @Column(name = "stripe_event_id", length = 64)
    private String stripeEventId;

    @Column(name = "stripe_usage_record_id", length = 64)
    private String stripeUsageRecordId;

    // === Importes ===
    private Integer quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "EUR";

    // === Estado ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BillingEventStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // === Metadata ===
    @Column(columnDefinition = "TEXT")
    private String metadata;
}

enum BillingEventType {
    LEAD_REGISTERED,
    LEAD_BILLED,
    USAGE_REPORTED,
    INVOICE_CREATED,
    INVOICE_FINALIZED,
    INVOICE_SENT,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    REFUND_ISSUED,
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_UPDATED,
    SUBSCRIPTION_CANCELLED,
    CUSTOMER_CREATED,
    CUSTOMER_UPDATED
}

enum BillingEventStatus {
    PENDING,
    SUCCESS,
    FAILED,
    RETRY,
    CANCELLED
}
```

---

### 9. **DataExport** (Exportaciones de Datos)

**Propósito**: Registro de exportaciones de datos para remarketing y GDPR.

```java
@Entity
@Table(name = "data_export",
    indexes = {
        @Index(name = "ix_dataexport_company", columnList = "company_id"),
        @Index(name = "ix_dataexport_created", columnList = "created_at"),
        @Index(name = "ix_dataexport_status", columnList = "status")
    })
public class DataExport extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_dataexport_company"))
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id",
        foreignKey = @ForeignKey(name = "fk_dataexport_user"))
    private User requestedBy;

    // === Tipo de exportación ===
    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 50)
    private ExportType exportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", nullable = false, length = 20)
    private ExportFormat exportFormat;

    // === Filtros aplicados ===
    @Column(name = "filters", columnDefinition = "TEXT")
    private String filters; // JSON con filtros aplicados
    
    /* Ejemplo:
    {
      "promotionIds": [1, 5, 7],
      "startDate": "2026-01-01",
      "endDate": "2026-01-31",
      "onlyVerified": true,
      "minLeadScore": 70
    }
    */

    // === Resultados ===
    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "file_url", length = 2048)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "expires_at")
    private Instant expiresAt; // URL temporal

    // === Estado ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExportStatus status = ExportStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // === Propósito (para auditoría GDPR) ===
    @Column(name = "purpose", length = 255)
    private String purpose; // "remarketing", "gdpr_request", "analytics"

    @Column(name = "downloaded_at")
    private Instant downloadedAt;

    @Column(name = "download_count")
    private Integer downloadCount = 0;
}

enum ExportType {
    CUSTOMERS,
    REDEMPTIONS,
    FULL_DATASET,
    GDPR_REQUEST,
    CUSTOM
}

enum ExportFormat {
    CSV,
    JSON,
    XLSX,
    XML
}

enum ExportStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED
}
```

---

### 10. **Device** (Actualizado con Geolocalización)

**Cambios**: Añadir todos los campos de ubicación.

```java
// Añadir estos campos a la entidad Device existente:

    // === Ubicación física ===
    @Column(name = "location_name", length = 255)
    private String locationName; // "Centro Comercial La Vaguada - Planta 2"
    
    @Column(name = "address", length = 255)
    private String address;
    
    @Column(name = "address_2", length = 255)
    private String address2;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "region", length = 100)
    private String region; // Comunidad Autónoma
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "country", length = 2)
    private String country; // ISO 3166-1 alpha-2
    
    @Column(precision = 10, scale = 7)
    private Double latitude;
    
    @Column(precision = 10, scale = 7)
    private Double longitude;
    
    @Column(name = "timezone", length = 50)
    private String timezone; // Europe/Madrid

    // === Hardware y estado ===
    @Column(length = 100)
    private String model;
    
    @Column(name = "os_version", length = 50)
    private String osVersion;
    
    @Column(name = "app_version", length = 20)
    private String appVersion;
    
    @Column(name = "storage_capacity_mb")
    private Integer storageCapacityMb;
    
    @Column(name = "battery_level")
    private Integer batteryLevel;

    // === Estado operativo ===
    private Boolean online = false;
    
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
    
    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    // === Métricas agregadas ===
    @Column(name = "total_impressions")
    private Long totalImpressions = 0L;
    
    @Column(name = "total_interactions")
    private Long totalInteractions = 0L;
    
    @Column(name = "total_redemptions")
    private Long totalRedemptions = 0L;

    // === Configuración ===
    private Boolean touchEnabled = true;
    private Boolean audioEnabled = true;
    
    @Column(name = "default_interval_seconds")
    private Integer defaultIntervalSeconds = 30;

enum DeviceStatus {
    ACTIVE,
    INACTIVE,
    MAINTENANCE,
    OFFLINE,
    DECOMMISSIONED
}
```

---

## 🔄 PLAN DE MIGRACIÓN

### Paso 1: Backup
```sql
-- Hacer backup completo de la BD
mysqldump -u user -p screenleads > backup_pre_migration.sql
```

### Paso 2: Crear nuevas tablas
```sql
-- Se crearán automáticamente con Hibernate al arrancar
-- o mediante Flyway/Liquibase
```

### Paso 3: Migrar datos
```sql
-- Migrar datos antiguos Customer/Client a nueva Customer
INSERT INTO customer (
    email, phone, first_name, last_name,
    created_at, updated_at
)
SELECT DISTINCT
    COALESCE(c.email, pl.email) as email,
    COALESCE(c.identifier, pl.phone) as phone,
    COALESCE(c.first_name, pl.first_name) as first_name,
    COALESCE(c.last_name, pl.last_name) as last_name,
    MIN(c.created_at) as created_at,
    NOW() as updated_at
FROM customer c
LEFT JOIN promotion_lead pl ON pl.customer_id = c.id
GROUP BY email, phone;

-- Migrar PromotionLead a PromotionRedemption
INSERT INTO promotion_redemption (
    promotion_id, customer_id, device_id,
    coupon_code, coupon_status,
    created_at, updated_at
)
SELECT
    pl.promotion_id,
    c.id as customer_id,
    -- device_id: necesitarás lógica para asignarlo
    pl.coupon_code,
    pl.coupon_status,
    pl.created_at,
    pl.updated_at
FROM promotion_lead pl
JOIN customer c ON c.email = pl.email;
```

### Paso 4: Verificar integridad
```sql
-- Verificar que no se perdieron datos
SELECT
    (SELECT COUNT(*) FROM customer) as old_customers,
    (SELECT COUNT(*) FROM customer) as new_customers,
    (SELECT COUNT(*) FROM promotion_lead) as old_leads,
    (SELECT COUNT(*) FROM promotion_redemption) as new_redemptions;
```

### Paso 5: Eliminar tablas antiguas
```sql
-- Solo después de verificar todo
DROP TABLE IF EXISTS promotion_lead;
DROP TABLE IF EXISTS customer;
DROP TABLE IF EXISTS client;
```

---

## 🎯 PRIORIDADES DE IMPLEMENTACIÓN

### FASE 1: Core (Semana 1-2)
1. ✅ Customer (nueva entidad que reemplaza antiguas Customer/Client)
2. ✅ Promotion (refactorizada)
3. ✅ PromotionRedemption
4. ✅ Device (actualizado)
5. Scripts de migración de datos

### FASE 2: Facturación (Semana 3-4)
6. ✅ CompanyBilling
7. ✅ Invoice
8. ✅ InvoiceItem
9. ✅ BillingEvent
10. Servicios de facturación

### FASE 3: Tracking (Semana 5)
11. ✅ UserAction
12. ✅ DataExport
13. Servicios de analytics

### FASE 4: Testing y Refinamiento (Semana 6)
14. Tests de integración
15. Documentación de APIs
16. Dashboard de métricas

---

## ✅ CHECKLIST DE VALIDACIÓN

- [ ] Customer unifica las antiguas Customer y Client
- [ ] Un cliente puede canjear múltiples promociones
- [ ] Histórico completo de acciones
- [ ] Múltiples métodos de autenticación
- [ ] Reglas de límites de canje funcionan
- [ ] Facturación mensual automatizada
- [ ] Auditoría completa de Stripe
- [ ] Exportaciones para remarketing
- [ ] Dispositivos geolocalizados
- [ ] GDPR compliant

---

**¿Comenzamos con la implementación?**
