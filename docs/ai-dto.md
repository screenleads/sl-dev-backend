# DTOs — snapshot incrustado

> Objetos de transferencia de datos.

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/web/dto/AdviceDTO.java
package com.screenleads.backend.app.web.dto;

import java.util.List;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdviceDTO {
    private Long id;
    private String description;
    private Boolean customInterval;
    /** Segundos (null si no aplica). */
    private Number interval;

    private MediaUpsertDTO media;       // record(Long id, String src)
    private PromotionRefDTO promotion;  // record(Long id)
    private CompanyRefDTO company;      // record(Long id, String name)

    /** Múltiples rangos de fechas con ventanas por día. */
    private List<AdviceScheduleDTO> schedules;
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/AdviceScheduleDTO.java
package com.screenleads.backend.app.web.dto;

import java.util.List;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdviceScheduleDTO {
    private Long id;
    private String startDate; // "YYYY-MM-DD" o null
    private String endDate;   // "YYYY-MM-DD" o null
    private List<AdviceTimeWindowDTO> windows;
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/AdviceTimeWindowDTO.java
package com.screenleads.backend.app.web.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdviceTimeWindowDTO {
    private Long id;
    private String weekday;  // MONDAY..SUNDAY
    private String fromTime; // "HH:mm[:ss]"
    private String toTime;   // "HH:mm[:ss]"
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/AdviceVisibilityRuleDTO.java
package com.screenleads.backend.app.web.dto;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record AdviceVisibilityRuleDTO(
  Long id,
  DayOfWeek day,
  LocalDate startDate,
  LocalDate endDate,
  Integer priority,
  List<TimeRangeDTO> ranges
) {}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/AppVersionDTO.java
package com.screenleads.backend.app.web.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersionDTO {

    private Long id;
    private String platform;
    private String version;
    private String message;
    private String url;
    private boolean forceUpdate;
}
```

```java
// src/main/java/com/screenleads/backend/app/web/dto/CompanyDTO.java
package com.screenleads.backend.app.web.dto;

import java.util.List;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.Media;

public record CompanyDTO(Long id, String name, String observations, Media logo, List<Device> devices,
        List<Advice> advices, String primaryColor, String secondaryColor) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/CompanyRefDTO.java
package com.screenleads.backend.app.web.dto;

public record CompanyRefDTO(Long id, String name) {
    public CompanyRefDTO(Long id) {
        this(id, null);
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/DeviceDTO.java
package com.screenleads.backend.app.web.dto;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.DeviceType;

public record DeviceDTO(Long id, String uuid, String descriptionName, Number width, Number height, DeviceType type,Company company) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/DeviceTypeDTO.java
package com.screenleads.backend.app.web.dto;

public record DeviceTypeDTO(Long id, String type, Boolean enabled) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/EntityInfo.java
package com.screenleads.backend.app.web.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class EntityInfo {
    private String entityName;
    private String className;
    private String tableName;
    private String idType;
    private Map<String, String> attributes = new LinkedHashMap<>();
    private Long rowCount; // null si no se pidió

    public EntityInfo() {
    }

    public EntityInfo(String entityName, String className, String tableName, String idType,
            Map<String, String> attributes, Long rowCount) {
        this.entityName = entityName;
        this.className = className;
        this.tableName = tableName;
        this.idType = idType;
        this.attributes = attributes;
        this.rowCount = rowCount;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/JwtResponse.java
package com.screenleads.backend.app.web.dto;

import com.screenleads.backend.app.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private User user;
}
```

```java
// src/main/java/com/screenleads/backend/app/web/dto/LeadSummaryDTO.java
package com.screenleads.backend.app.web.dto;

import java.time.LocalDate;
import java.util.Map;

public record LeadSummaryDTO(
        Long promotionId,
        long totalLeads,
        long uniqueIdentifiers, // únicos según identifier (email/phone normalizado)
        Map<LocalDate, Long> leadsByDay // YYYY-MM-DD -> conteo
) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/LoginRequest.java
package com.screenleads.backend.app.web.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
```

```java
// src/main/java/com/screenleads/backend/app/web/dto/MediaDTO.java
package com.screenleads.backend.app.web.dto;

import com.screenleads.backend.app.domain.model.MediaType;

public record MediaDTO(Long id, String src, MediaType type) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/MediaTypeDTO.java
package com.screenleads.backend.app.web.dto;

public record MediaTypeDTO(Long id, String extension, String type, Boolean enabled) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/MediaUpsertDTO.java
package com.screenleads.backend.app.web.dto;

public record MediaUpsertDTO(Long id, String src) {
}
```

```java
// src/main/java/com/screenleads/backend/app/web/dto/PasswordChangeRequest.java
package com.screenleads.backend.app.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeRequest {
    private String currentPassword;
    private String newPassword;
}
```

```java
// src/main/java/com/screenleads/backend/app/web/dto/PromotionDTO.java
package com.screenleads.backend.app.web.dto;

import com.screenleads.backend.app.domain.model.LeadIdentifierType;
import com.screenleads.backend.app.domain.model.LeadLimitType;

public record PromotionDTO(Long id,
        String legal_url,
        String url,
        String description,
        String templateHtml,
        LeadLimitType leadLimitType,
        LeadIdentifierType leadIdentifierType) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/PromotionLeadDTO.java
package com.screenleads.backend.app.web.dto;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record PromotionLeadDTO(
        Long id,
        Long promotionId,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate,
        ZonedDateTime acceptedPrivacyAt,
        ZonedDateTime acceptedTermsAt,
        ZonedDateTime createdAt) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/PromotionRefDTO.java
package com.screenleads.backend.app.web.dto;

public record PromotionRefDTO(Long id) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/RegisterRequest.java
package com.screenleads.backend.app.web.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String lastName;
    private String username;
    private String email;
    private String password;
    Long companyId;
}
```

```java
// src/main/java/com/screenleads/backend/app/web/dto/RoleDTO.java
// com.screenleads.backend.app.web.dto.RoleDTO
package com.screenleads.backend.app.web.dto;

public record RoleDTO(
                Long id,
                String role,
                String description,
                Integer level,

                boolean userRead, boolean userCreate, boolean userUpdate, boolean userDelete,
                boolean companyRead, boolean companyCreate, boolean companyUpdate, boolean companyDelete,
                boolean deviceRead, boolean deviceCreate, boolean deviceUpdate, boolean deviceDelete,
                boolean deviceTypeRead, boolean deviceTypeCreate, boolean deviceTypeUpdate, boolean deviceTypeDelete,
                boolean mediaRead, boolean mediaCreate, boolean mediaUpdate, boolean mediaDelete,
                boolean mediaTypeRead, boolean mediaTypeCreate, boolean mediaTypeUpdate, boolean mediaTypeDelete,
                boolean promotionRead, boolean promotionCreate, boolean promotionUpdate, boolean promotionDelete,
                boolean adviceRead, boolean adviceCreate, boolean adviceUpdate, boolean adviceDelete,
                boolean appVersionRead, boolean appVersionCreate, boolean appVersionUpdate, boolean appVersionDelete) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/TimeRangeDTO.java
package com.screenleads.backend.app.web.dto;

import java.time.LocalTime;

public record TimeRangeDTO(
  Long id,
  LocalTime from,
  LocalTime to
) {}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/UserDto.java
package com.screenleads.backend.app.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor // ← necesario para deserializar POST/PUT
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String username;
    private String password;
    private String name;
    private String lastName;
    private Long companyId; // sólo id de la empresa
    private List<String> roles; // nombres de rol, p.e. ["ROLE_ADMIN","ROLE_COMPANY_VIEWER"]
}

```

