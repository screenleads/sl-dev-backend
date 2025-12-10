# Entidades JPA — snapshot incrustado

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada entidad para revisión.

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
}```

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
}```

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
}```

```java
// src/main/java/com/screenleads/backend/app/domain/model/ApiKey.java
package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client", nullable = false)
    private Client client;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private String permissions; // Puedes usar un Set<String> o relación si lo prefieres

    @Column(name = "company_scope")
    private Long companyScope; // NULL = acceso global, ID = compañía específica

    private String description; // Descripción de la API Key

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public Long getCompanyScope() {
        return companyScope;
    }

    public void setCompanyScope(Long companyScope) {
        this.companyScope = companyScope;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/AppEntity.java
package com.screenleads.backend.app.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_entity", uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_entity_resource", columnNames = "resource"),
                @UniqueConstraint(name = "uk_app_entity_endpoint", columnNames = "endpoint_base")
}, indexes = {
                @Index(name = "ix_app_entity_endpoint", columnList = "endpoint_base"),
                @Index(name = "ix_app_entity_sort_order", columnList = "sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /** Clave funcional: "company", "device", "promotion_lead", ... */
        @Column(nullable = false, length = 80)
        private String resource;

        /** Nombre "técnico" mostrable (normalmente igual al nombre de clase simple). */
        @Column(name = "entity_name", nullable = false, length = 120)
        private String entityName;

        /** Nombre FQCN (opcional). */
        @Column(name = "class_name", length = 300)
        private String className;

        /** Nombre de tabla física (opcional). */
        @Column(name = "table_name", length = 120)
        private String tableName;

        /** Tipo del ID: "Long", "UUID", etc. */
        @Column(name = "id_type", length = 60)
        private String idType;

        /** Endpoint base REST sin /api (p. ej., "/users"). */
        @Column(name = "endpoint_base", nullable = false, length = 120)
        private String endpointBase;

        /** Niveles de permiso (si aplican en tu política). */
        @Column(name = "create_level", nullable = false)
        private Integer createLevel;

        @Column(name = "read_level", nullable = false)
        private Integer readLevel;

        @Column(name = "update_level", nullable = false)
        private Integer updateLevel;

        @Column(name = "delete_level", nullable = false)
        private Integer deleteLevel;

        @Column(name = "visible_in_menu")
        private Boolean visibleInMenu;

        /** Conteo de filas (opcional; null si no se pide). */
        @Column(name = "row_count")
        private Long rowCount;

        /** Texto mostrado en el dashboard/menú (p.ej., "Devices"). */
        @Column(name = "display_label", nullable = false, length = 120)
        private String displayLabel;

        /** Nombre de icono (p.ej., "tv-2", "building-2", "image"). */
        @Column(name = "icon", length = 80)
        private String icon;

        /** Orden en el menú (más bajo = antes). */
        @Column(name = "sort_order")
        private Integer sortOrder;

        // AppEntity.java (solo la parte relevante)
        @OneToMany(mappedBy = "appEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        @OrderBy("listOrder ASC NULLS LAST, id ASC")
        @Builder.Default
        private java.util.List<AppEntityAttribute> attributes = new java.util.ArrayList<>();

}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/AppEntityAttribute.java
// AppEntityAttribute.java
package com.screenleads.backend.app.domain.model;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_entity_attribute", uniqueConstraints = @UniqueConstraint(columnNames = { "app_entity_id",
        "attr_name" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppEntityAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_entity_id")
    private AppEntity appEntity;

    @Column(name = "attr_name", nullable = false, length = 100)
    private String name; // clave (ej: "name", "primaryColor")

    @Column(name = "attr_type", length = 80)
    private String attrType; // compat con lo que tenías (String, Long, Boolean...)

    // Metadatos extendidos
    @Column(name = "data_type", length = 50)
    private String dataType; // opcional si quieres diferenciarlo de attrType

    @Column(name = "relation_target", length = 80)
    private String relationTarget; // ej: "Company", "Media"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enum_values", columnDefinition = "jsonb")
    private List<String> enumValues; // o JsonNode / List<Map<String,Object>>

    // LIST
    @Column(name = "list_visible")
    @Builder.Default
    private Boolean listVisible = Boolean.TRUE;

    @Column(name = "list_order")
    private Integer listOrder;

    @Column(name = "list_label", length = 150)
    private String listLabel;

    @Column(name = "list_width_px")
    private Integer listWidthPx;

    @Column(name = "list_align", length = 10)
    private String listAlign;

    @Column(name = "list_searchable")
    @Builder.Default
    private Boolean listSearchable = Boolean.TRUE;

    @Column(name = "list_sortable")
    @Builder.Default
    private Boolean listSortable = Boolean.TRUE;

    // FORM
    @Column(name = "form_visible")
    @Builder.Default
    private Boolean formVisible = Boolean.TRUE;

    @Column(name = "form_order")
    private Integer formOrder;

    @Column(name = "form_label", length = 150)
    private String formLabel;

    @Column(name = "control_type", length = 30)
    private String controlType;

    @Column(name = "placeholder", length = 200)
    private String placeholder;

    @Column(name = "help_text", length = 300)
    private String helpText;

    @Column(name = "required")
    @Builder.Default
    private Boolean required = Boolean.FALSE;

    @Column(name = "read_only")
    @Builder.Default
    private Boolean readOnly = Boolean.FALSE;

    @Column(name = "min_num", precision = 18, scale = 6)
    private BigDecimal minNum;

    @Column(name = "max_num", precision = 18, scale = 6)
    private BigDecimal maxNum;

    @Column(name = "min_len")
    private Integer minLen;

    @Column(name = "max_len")
    private Integer maxLen;

    @Column(name = "pattern", length = 200)
    private String pattern;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "options_endpoint", length = 200)
    private String optionsEndpoint;
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
}```

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
}```

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
}```

```java
// src/main/java/com/screenleads/backend/app/domain/model/Client.java
package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String clientId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<ApiKey> apiKeys;

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<ApiKey> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<ApiKey> apiKeys) {
        this.apiKeys = apiKeys;
    }
}
```

```java
// src/main/java/com/screenleads/backend/app/domain/model/Company.java

package com.screenleads.backend.app.domain.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Table(name = "company", indexes = {
        @Index(name = "ix_company_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends Auditable {
    public enum BillingStatus {
        INCOMPLETE,
        ACTIVE,
        PAST_DUE,
        CANCELED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "observations", length = 1000)
    private String observations;

    @Column(name = "primary_color", length = 7)
    private String primaryColor; // ej: #FFFFFF

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_id", foreignKey = @ForeignKey(name = "fk_company_logo"))
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

    // Stripe & Billing fields
    @Column(name = "stripe_customer_id", length = 64)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 64)
    private String stripeSubscriptionId;

    @Column(name = "stripe_subscription_item_id", length = 64)
    private String stripeSubscriptionItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_status", length = 32, nullable = false)
    @Builder.Default
    private BillingStatus billingStatus = BillingStatus.INCOMPLETE;

    public void setBillingStatus(String status) {
        this.billingStatus = BillingStatus.valueOf(status);
    }
}```

```java
// src/main/java/com/screenleads/backend/app/domain/model/CompanyToken.java
package com.screenleads.backend.app.domain.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "company_token")
public class CompanyToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private String role;

    @Column(nullable = true, length = 255)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // Getters and setters
    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
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
@Table(name = "device", indexes = {
        @Index(name = "ix_device_uuid", columnList = "uuid", unique = true),
        @Index(name = "ix_device_company", columnList = "company_id"),
        @Index(name = "ix_device_type", columnList = "type_id")
})
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

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(name = "description_name", length = 255)
    private String descriptionName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_device_company"))
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_device_type"))
    private DeviceType type;

    @ManyToMany
    @JoinTable(name = "device_advice", joinColumns = @JoinColumn(name = "device_id"), inverseJoinColumns = @JoinColumn(name = "advice_id"))
    private Set<Advice> advices;
}```

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
}```

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
}```

```java
// src/main/java/com/screenleads/backend/app/domain/model/LeadIdentifierType.java
package com.screenleads.backend.app.domain.model;


public enum LeadIdentifierType {
EMAIL,
PHONE,
DOCUMENT,
OTHER
}```

```java
// src/main/java/com/screenleads/backend/app/domain/model/LeadLimitType.java
package com.screenleads.backend.app.domain.model;

public enum LeadLimitType {
    NO_LIMIT,
    ONE_PER_PERSON,
    ONE_PER_24H,
    CUSTOM_TIME
}```

```java
// src/main/java/com/screenleads/backend/app/domain/model/Media.java
package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "media", indexes = {
        @Index(name = "ix_media_company", columnList = "company_id"),
        @Index(name = "ix_media_created_at", columnList = "created_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_media_src", columnNames = { "src" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String src;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_media_type"))
    private MediaType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_media_company"))
    private Company company;
}```

```java
// src/main/java/com/screenleads/backend/app/domain/model/MediaType.java
package com.screenleads.backend.app.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Table(name = "media_type", uniqueConstraints = @UniqueConstraint(name = "uk_mediatype_type", columnNames = "type"))
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
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(nullable = false, length = 50)
    private String type; // e.g., IMAGE, VIDEO

    @Column(nullable = false, length = 10)
    private String extension; // e.g., jpg, mp4
}```

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
@org.hibernate.annotations.Filter(name = "companyFilter", condition = "company_id = :companyId")
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
import org.hibernate.annotations.ParamDef;```

