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
// src/main/java/com/screenleads/backend/app/web/dto/AppEntityDTO.java
package com.screenleads.backend.app.web.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AppEntityDTO {

    private Long id;

    private String resource;
    private String entityName;
    private String className;
    private String tableName;
    private String idType;
    private String endpointBase;

    private Integer createLevel;
    private Integer readLevel;
    private Integer updateLevel;
    private Integer deleteLevel;

    private Long rowCount;

    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();

    // Dashboard metadata
    private String displayLabel;
    private String icon;
    private Integer sortOrder;
}

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
// src/main/java/com/screenleads/backend/app/web/dto/JwtResponse.java
// src/main/java/com/screenleads/backend/app/web/dto/JwtResponse.java
package com.screenleads.backend.app.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtResponse {
    @Builder.Default
    private String tokenType = "Bearer";
    private String accessToken;
    private String refreshToken;
    private UserDto user;
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
// src/main/java/com/screenleads/backend/app/web/dto/LoginRequest.java
package com.screenleads.backend.app.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank
    private String username;
    @NotBlank
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
// src/main/java/com/screenleads/backend/app/web/dto/PasswordChangeRequest.java
package com.screenleads.backend.app.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordChangeRequest {
    @NotBlank
    private String currentPassword;
    @NotBlank
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
// src/main/java/com/screenleads/backend/app/web/dto/RegisterRequest.java
package com.screenleads.backend.app.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank
    private String username;
    @Email
    private String email;
    @NotBlank
    private String password;
    private String name;
    private String lastName;
    private Long companyId; // opcional: si lo usas al registrar
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
        Integer level) {
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.screenleads.backend.app.domain.model.Role;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String lastName;
    private Long companyId;
    private Role role;

    // solo para crear/actualizar; no lo rellenes al responder
    private String password;
}

```

