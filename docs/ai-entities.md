# Entidades JPA — snapshot incrustado

> Clases de dominio (model/entity).

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/domain/model/Advice.java
package com.screenleads.backend.app.domain.model;


import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.Filter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(
name = "advice",
indexes = {
@Index(name = "ix_advice_company", columnList = "company_id"),
@Index(name = "ix_advice_media", columnList = "media_id"),
@Index(name = "ix_advice_promotion", columnList = "promotion_id")
}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Filter(name = "companyFilter", condition = "company_id = :companyId")
public class Advice extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(length = 512)
private String description;


@Column(name = "custom_interval", nullable = false)
@Builder.Default
private Boolean customInterval = Boolean.FALSE;


/** Almacenada como segundos vía DurationToLongConverter */
@Column(name = "interval_seconds")
private Duration interval;


@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "company_id", nullable = false,
foreignKey = @ForeignKey(name = "fk_advice_company"))
@JsonIgnore
private Company company;


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "media_id",
foreignKey = @ForeignKey(name = "fk_advice_media"))
private Media media;


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "promotion_id",
foreignKey = @ForeignKey(name = "fk_advice_promotion"))
private Promotion promotion;


/** Rango(s) de fechas y ventanas por día */
@OneToMany(mappedBy = "advice", cascade = CascadeType.ALL, orphanRemoval = true)
private List<AdviceSchedule> schedules;


@ManyToMany(mappedBy = "advices")
@JsonIgnore
private Set<Device> devices;
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/AdviceSchedule.java
package com.screenleads.backend.app.domain.model;


import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "advice_schedule",
indexes = {
@Index(name = "ix_adviceschedule_dates", columnList = "start_date,end_date"),
@Index(name = "ix_adviceschedule_advice", columnList = "advice_id")
}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdviceSchedule extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(name = "start_date")
private LocalDate startDate; // nullable → sin límite inferior


@Column(name = "end_date")
private LocalDate endDate; // nullable → sin límite superior


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "advice_id",
foreignKey = @ForeignKey(name = "fk_adviceschedule_advice"))
@JsonIgnore
private Advice advice;


@OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
private List<AdviceTimeWindow> windows;
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/AdviceTimeWindow.java
package com.screenleads.backend.app.domain.model;


import java.time.DayOfWeek;
import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "advice_time_window",
indexes = @Index(name = "ix_advicetimewindow_schedule", columnList = "schedule_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdviceTimeWindow extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 10)
private DayOfWeek weekday;


/** Intervalo [from,to) (fin exclusivo) */
@Column(name = "from_time", nullable = false)
private LocalTime fromTime;


@Column(name = "to_time", nullable = false)
private LocalTime toTime;


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "schedule_id",
foreignKey = @ForeignKey(name = "fk_advicetimewindow_schedule"))
@JsonIgnore
private AdviceSchedule schedule;
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/AppEntity.java
// src/main/java/com/screenleads/backend/app/domain/model/AppEntity.java
package com.screenleads.backend.app.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "entity",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_entity_resource", columnNames = "resource"),
        @UniqueConstraint(name = "uk_entity_endpoint", columnNames = "endpoint_base")
    },
    indexes = {
        @Index(name = "ix_entity_endpoint", columnList = "endpoint_base")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AppEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Clave funcional: "company", "device", "promotion_lead", ... */
    @Column(nullable = false, length = 80)
    private String resource;

    /** Nombre mostrado en la tabla (p. ej. "Company"). */
    @Column(name = "entity_name", nullable = false, length = 120)
    private String entityName;

    /** Nombre FQCN de la clase JPA (si aplica). */
    @Column(name = "class_name", length = 300)
    private String className;

    /** Nombre de tabla física (si aplica). */
    @Column(name = "table_name", length = 120)
    private String tableName;

    /** Tipo de ID (p. ej. "Long"). */
    @Column(name = "id_type", length = 60)
    private String idType;

    /** Endpoint base REST sin /api (p. ej. "/users"). */
    @Column(name = "endpoint_base", nullable = false, length = 120)
    private String endpointBase;

    /** Niveles de permiso (si los usas). */
    @Column(name = "create_level", nullable = false)
    private Integer createLevel;

    @Column(name = "read_level", nullable = false)
    private Integer readLevel;

    @Column(name = "update_level", nullable = false)
    private Integer updateLevel;

    @Column(name = "delete_level", nullable = false)
    private Integer deleteLevel;

    /** Conteo de filas (opcional, puede quedar null) */
    @Column(name = "row_count")
    private Long rowCount;

    /** Atributos: nombre → tipo (guardados en tabla secundaria) */
    @ElementCollection
    @CollectionTable(
        name = "entity_attribute",
        joinColumns = @JoinColumn(name = "entity_id",
            foreignKey = @ForeignKey(name = "fk_entity_attribute_entity"))
    )
    @MapKeyColumn(name = "attr_name", length = 120)
    @Column(name = "attr_type", length = 200)
    @OrderColumn(name = "attr_order")
    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/model/AppVersion.java
package com.screenleads.backend.app.domain.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "app_version",
uniqueConstraints = @UniqueConstraint(
name = "uk_appversion_platform_version",
columnNames = {"platform", "version"}
),
indexes = @Index(name = "ix_appversion_platform", columnList = "platform")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersion extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(nullable = false, length = 20)
private String platform; // "android" o "ios"


@Column(nullable = false, length = 40)
private String version;


@Column(length = 255)
private String message;


@Column(nullable = false, length = 2048)
private String url;


@Column(name = "force_update", nullable = false)
private boolean forceUpdate;
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/Auditable.java
package com.screenleads.backend.app.domain.model;


import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;


@MappedSuperclass
@Getter
@Setter
public abstract class Auditable {
@CreationTimestamp
@Column(name = "created_at", updatable = false, nullable = false)
private Instant createdAt;


@UpdateTimestamp
@Column(name = "updated_at")
private Instant updatedAt;
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/ChatMessage.java
package com.screenleads.backend.app.domain.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
public enum MessageType { REFRESH_ADS, RESTART_APP, MAINTENANCE_MODE, NOTIFY }
private String id;
private MessageType type;
private String message;
private String senderId;
private String senderName;
private String roomId;
private Instant timestamp;
private Map<String, Object> metadata;
private boolean systemGenerated;
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/Company.java
package com.screenleads.backend.app.domain.model;


import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "company",
indexes = {
@Index(name = "ix_company_name", columnList = "name")
}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(nullable = false, length = 120)
private String name;


@Column(name="observations", length=1000)
private String observations;

@Column(name = "primary_color", length = 7)
private String primaryColor; // ej: #FFFFFF


@Column(name = "secondary_color", length = 7)
private String secondaryColor;


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "logo_id",
foreignKey = @ForeignKey(name = "fk_company_logo"))
private Media logo;


@OneToMany(mappedBy = "company")
@JsonIgnore
private List<Device> devices;


@OneToMany(mappedBy = "company")
@JsonIgnore
private List<Advice> advices;


@OneToMany(mappedBy = "company")
@JsonIgnore
private List<User> users;
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/CouponStatus.java
package com.screenleads.backend.app.domain.model;

public enum CouponStatus {
    NEW,        // generado pero aún no validado
    VALID,      // validado / listo para canjear
    REDEEMED,   // ya canjeado
    EXPIRED,    // caducado por fecha o manualmente
    CANCELLED   // invalidado manualmente / antifraude
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/model/Customer.java
package com.screenleads.backend.app.domain.model;

import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_customer_company_identifier",
            columnNames = {"company_id", "identifier_type", "identifier"}
        )
    },
    indexes = {
        @Index(name = "ix_customer_company", columnList = "company_id"),
        @Index(name = "ix_customer_identifier", columnList = "identifier")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cliente pertenece a una empresa (la misma de la promoción)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_customer_company"))
    private Company company;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 20)
    private LeadIdentifierType identifierType;

    @Column(name = "identifier", nullable = false, length = 255)
    private String identifier; // normalizado (email lowercase, phone E.164, etc.)

    @OneToMany(mappedBy = "customer")
    private Set<PromotionLead> leads;
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/model/Device.java
package com.screenleads.backend.app.domain.model;


import java.util.Set;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "device",
indexes = {
@Index(name = "ix_device_uuid", columnList = "uuid", unique = true),
@Index(name = "ix_device_company", columnList = "company_id"),
@Index(name = "ix_device_type", columnList = "type_id")
}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(nullable = false, unique = true, length = 64)
private String uuid;


@Column(length = 120)
private String name;


@Column(nullable = false)
private Integer width;


@Column(nullable = false)
private Integer height;


@Column(name="description_name", length=255)
private String descriptionName;


@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "company_id", nullable = false,
foreignKey = @ForeignKey(name = "fk_device_company"))
private Company company;


@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "type_id", nullable = false,
foreignKey = @ForeignKey(name = "fk_device_type"))
private DeviceType type;


@ManyToMany
@JoinTable(name = "device_advice",
joinColumns = @JoinColumn(name = "device_id"),
inverseJoinColumns = @JoinColumn(name = "advice_id"))
private Set<Advice> advices;
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/DeviceType.java
package com.screenleads.backend.app.domain.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "device_type",
uniqueConstraints = @UniqueConstraint(name = "uk_devicetype_type", columnNames = "type")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceType {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(nullable = false, length = 50)
private String type;


@Column(nullable = false)
@Builder.Default
private Boolean enabled = Boolean.TRUE;
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/DurationToLongConverter.java
package com.screenleads.backend.app.domain.model;


import java.time.Duration;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class DurationToLongConverter implements AttributeConverter<Duration, Long> {
@Override
public Long convertToDatabaseColumn(Duration attribute) {
return attribute == null ? null : attribute.getSeconds();
}
@Override
public Duration convertToEntityAttribute(Long dbData) {
return dbData == null ? null : Duration.ofSeconds(dbData);
}
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/LeadIdentifierType.java
package com.screenleads.backend.app.domain.model;


public enum LeadIdentifierType {
EMAIL,
PHONE,
DOCUMENT,
OTHER
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/LeadLimitType.java
package com.screenleads.backend.app.domain.model;

public enum LeadLimitType {
    NO_LIMIT,
    ONE_PER_PERSON,
    ONE_PER_24H,
    CUSTOM_TIME
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/Media.java
package com.screenleads.backend.app.domain.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "media",
indexes = {
@Index(name = "ix_media_company", columnList = "company_id"),
@Index(name = "ix_media_created_at", columnList = "created_at")
},
uniqueConstraints = @UniqueConstraint(name = "uk_media_src", columnNames = {"src"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(nullable = false, length = 255)
private String name;


@Column(nullable = false, length = 2048)
private String src;


@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "type_id", nullable = false,
foreignKey = @ForeignKey(name = "fk_media_type"))
private MediaType type;


@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "company_id", nullable = false,
foreignKey = @ForeignKey(name = "fk_media_company"))
private Company company;
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/MediaType.java
package com.screenleads.backend.app.domain.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "media_type",
uniqueConstraints = @UniqueConstraint(name = "uk_mediatype_type", columnNames = "type")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaType {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Builder.Default
@Column(name="enabled", nullable=false)
private Boolean enabled = Boolean.TRUE;

@Column(nullable = false, length = 50)
private String type; // e.g., IMAGE, VIDEO


@Column(nullable = false, length = 10)
private String extension; // e.g., jpg, mp4
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/Promotion.java
package com.screenleads.backend.app.domain.model;

import java.time.Instant;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "promotion",
    indexes = {
        @Index(name = "ix_promotion_company", columnList = "company_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    // URL legal (camelCase en el modelo, snake_case en DB)
    @Column(name = "legal_url", length = 2048)
    private String legalUrl;

    @Column(length = 2048)
    private String url;

    @Lob
    @Column(name = "template_html")
    private String templateHtml;

    // === NUEVO: cupón externo para enlazar con el negocio final
    @Column(name = "external_coupon_code", length = 120)
    private String externalCouponCode;

    // === NUEVO: ventana temporal de la promoción
    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    // Reglas de identificación y límites
    @Enumerated(EnumType.STRING)
    @Column(name = "lead_identifier_type", length = 30, nullable = false)
    @Builder.Default
    private LeadIdentifierType leadIdentifierType = LeadIdentifierType.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_limit_type", length = 30, nullable = false)
    @Builder.Default
    private LeadLimitType leadLimitType = LeadLimitType.NO_LIMIT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "company_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_promotion_company")
    )
    private Company company;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PromotionLead> leads;
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/model/PromotionLead.java
package com.screenleads.backend.app.domain.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "promotion_lead",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_promotionlead_promotion_identifier",
            columnNames = {"promotion_id", "identifier"}
        ),
        @UniqueConstraint(
            name = "uk_promotionlead_coupon_code",
            columnNames = {"coupon_code"}
        )
    },
    indexes = {
        @Index(name = "ix_promotionlead_promotion", columnList = "promotion_id"),
        @Index(name = "ix_promotionlead_created_at", columnList = "created_at"),
        @Index(name = "ix_promotionlead_coupon_status", columnList = "coupon_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class PromotionLead extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Relaciones ===
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "promotion_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_promotionlead_promotion")
    )
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "customer_id",
        foreignKey = @ForeignKey(name = "fk_promotionlead_customer")
    )
    private Customer customer;

    // === Datos personales (opcionales) ===
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    private LocalDate birthDate;

    // Consentimientos
    private Instant acceptedPrivacyAt;
    private Instant acceptedTermsAt;

    // === Identificación del lead dentro de la promo ===
    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 20)
    private LeadIdentifierType identifierType;

    @Column(nullable = false, length = 255)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", length = 20)
    private LeadLimitType limitType;

    // === Cupón interno generado por el sistema ===
    @Column(name = "coupon_code", length = 64, nullable = false)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_status", length = 20, nullable = false)
    @Builder.Default
    private CouponStatus couponStatus = CouponStatus.VALID;

    // Cuándo caduca este cupón concreto (si aplica) y cuándo fue canjeado
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/model/Role.java
// src/main/java/com/screenleads/backend/app/domain/model/Role.java
package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role", indexes = @Index(name = "ix_role_role", columnList = "role", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Nombre técnico, p.ej. "ROLE_ADMIN" */
  @Column(nullable = false, unique = true, length = 60)
  private String role;

  /** Descripción legible */
  @Column(length = 255)
  private String description;

  /** Nivel de poder: 1 = máximo privilegio, 2 = menos, ... */
  @Column(nullable = false)
  private Integer level;
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/model/User.java
// src/main/java/com/screenleads/backend/app/domain/model/User.java
package com.screenleads.backend.app.domain.model;

import java.util.Collection;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "app_user", indexes = {
        @Index(name = "ix_user_username", columnList = "username", unique = true),
        @Index(name = "ix_user_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User extends Auditable implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @Email
    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(length = 100)
    private String name;

    @Column(name = "last_name", length = 100)
    private String lastName;

    /** 🔁 Cambiamos de ManyToMany a ManyToOne */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_user_role"))
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", foreignKey = @ForeignKey(name = "fk_user_company"))
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_id", foreignKey = @ForeignKey(name = "fk_user_profile_image"))
    private Media profileImage;

    // === UserDetails ===
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Un único rol → una única authority
        String authority = (role != null && role.getRole() != null) ? role.getRole() : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/domain/model/package-info.java
@FilterDef(name = "companyFilter", parameters = @ParamDef(name = "companyId", type = Long.class) // o type = "long"
)
package com.screenleads.backend.app.domain.model;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
```

