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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

public class AdviceScheduleDTO {
    private Long id;
    private String startDate; // "YYYY-MM-DD" o null
    private String endDate;   // "YYYY-MM-DD" o null
    private List<AdviceTimeWindowDTO> windows;

    // Para compatibilidad con payloads agrupados por día
    @JsonProperty("dayWindows")
    private List<DayWindowDTO> dayWindows;

    public AdviceScheduleDTO() {}

    public AdviceScheduleDTO(Long id, String startDate, String endDate, List<AdviceTimeWindowDTO> windows, List<DayWindowDTO> dayWindows) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.windows = windows;
        this.dayWindows = dayWindows;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public List<AdviceTimeWindowDTO> getWindows() { return windows; }
    public void setWindows(List<AdviceTimeWindowDTO> windows) { this.windows = windows; }
    public List<DayWindowDTO> getDayWindows() { return dayWindows; }
    public void setDayWindows(List<DayWindowDTO> dayWindows) { this.dayWindows = dayWindows; }

    public static class DayWindowDTO {
        private String weekday;
        private List<RangeDTO> ranges;

        public DayWindowDTO() {}
        public DayWindowDTO(String weekday, List<RangeDTO> ranges) {
            this.weekday = weekday;
            this.ranges = ranges;
        }
        public String getWeekday() { return weekday; }
        public void setWeekday(String weekday) { this.weekday = weekday; }
        public List<RangeDTO> getRanges() { return ranges; }
        public void setRanges(List<RangeDTO> ranges) { this.ranges = ranges; }
    }

    public static class RangeDTO {
        private String fromTime;
        private String toTime;

        public RangeDTO() {}
        public RangeDTO(String fromTime, String toTime) {
            this.fromTime = fromTime;
            this.toTime = toTime;
        }
        public String getFromTime() { return fromTime; }
        public void setFromTime(String fromTime) { this.fromTime = fromTime; }
        public String getToTime() { return toTime; }
        public void setToTime(String toTime) { this.toTime = toTime; }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdviceScheduleDTO that = (AdviceScheduleDTO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate) &&
                Objects.equals(windows, that.windows) &&
                Objects.equals(dayWindows, that.dayWindows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startDate, endDate, windows, dayWindows);
    }
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
// src/main/java/com/screenleads/backend/app/web/dto/AppEntityAttributeDTO.java
// src/main/java/com/screenleads/backend/app/web/dto/EntityAttributeDTO.java
package com.screenleads.backend.app.web.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
public record AppEntityAttributeDTO(
        // Identidad + tipado
        Long id,
        String name,
        String attrType, // e.g. "BASIC", "RELATION", "ENUM"
        String dataType, // e.g. "String", "Long", "Boolean", "Duration", etc.
        String relationTarget, // si RELATION: entidad destino

        // *** NUEVO: lista nativa para enums ***
        List<String> enumValues,

        // (OPCIONAL) compatibilidad si tu front ya envía JSON crudo
        String enumValuesJson,

        // List / tabla
        Boolean listVisible,
        Integer listOrder,
        String listLabel,
        Integer listWidthPx,
        String listAlign,
        Boolean listSearchable,
        Boolean listSortable,

        // Form
        Boolean formVisible,
        Integer formOrder,
        String formLabel,
        String controlType, // input, select, textarea, color, date, time, etc.
        String placeholder,
        String helpText,
        Boolean required,
        Boolean readOnly,

        // Validaciones
        BigDecimal minNum,
        BigDecimal maxNum,
        Integer minLen,
        Integer maxLen,
        String pattern,

        // Otros
        String defaultValue,
        String optionsEndpoint) {
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/AppEntityDTO.java
package com.screenleads.backend.app.web.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder(toBuilder = true)
public record AppEntityDTO(
        Long id,
        String resource,
        String entityName,
        String className,
        String tableName,
        String idType,
        String endpointBase,
        Integer createLevel,
        Integer readLevel,
        Integer updateLevel,
        Integer deleteLevel,
        Boolean visibleInMenu,
        Long rowCount,
        String displayLabel,
        String icon,
        Integer sortOrder,
        List<EntityAttributeDTO> attributes) {
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

public record CompanyDTO(Long id, String name, String observations, MediaSlimDTO logo, List<Device> devices,
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
// src/main/java/com/screenleads/backend/app/web/dto/EntityAttributeDTO.java
package com.screenleads.backend.app.web.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
public record EntityAttributeDTO(
        // Identidad + tipado
        Long id,
        String name,
        String attrType, // e.g. "BASIC", "RELATION", "ENUM"
        String dataType, // e.g. "String", "Long", "Boolean", "Duration", etc.
        String relationTarget, // si RELATION: nombre entidad destino
        String enumValuesJson, // si ENUM: JSON con valores

        // List / tabla
        Boolean listVisible,
        Integer listOrder,
        String listLabel,
        Integer listWidthPx,
        String listAlign,
        Boolean listSearchable,
        Boolean listSortable,

        // Form
        Boolean formVisible,
        Integer formOrder,
        String formLabel,
        String controlType, // input, select, textarea, color, date, time, etc.
        String placeholder,
        String helpText,
        Boolean required,
        Boolean readOnly,

        // Validaciones
        BigDecimal minNum,
        BigDecimal maxNum,
        Integer minLen,
        Integer maxLen,
        String pattern,

        // Otros
        String defaultValue,
        String optionsEndpoint) {
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
// src/main/java/com/screenleads/backend/app/web/dto/MediaSlimDTO.java
package com.screenleads.backend.app.web.dto;

import java.time.Instant;

public record MediaSlimDTO(
        Long id,
        String src,
        MediaTypeDTO type,
        Instant createdAt,
        Instant updatedAt) {
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
// src/main/java/com/screenleads/backend/app/web/dto/ReorderRequest.java
package com.screenleads.backend.app.web.dto;

import java.util.List;

public record ReorderRequest(List<Long> ids) {
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
// src/main/java/com/screenleads/backend/app/web/dto/UserCreationResponse.java
package com.screenleads.backend.app.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreationResponse {
    private UserDto user;
    private String tempPassword;
}

```

```java
// src/main/java/com/screenleads/backend/app/web/dto/UserDto.java
package com.screenleads.backend.app.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;


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
    private RoleDTO role; // Ahora es objeto

    // Nuevo: referencia a compañía como objeto
    private CompanyRefDTO company;

    // Nuevo: imagen de perfil como objeto slim
    private MediaSlimDTO profileImage;

    // solo para crear/actualizar; no lo rellenes al responder
    private String password;
}

```

