# Mappers — snapshot incrustado

> MapStruct u otros mapeadores.

> Snapshot generado desde la rama `develop`. Contiene el **código completo** de cada archivo.

---

```java
// src/main/java/com/screenleads/backend/app/web/mapper/AdviceMapper.java
package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.AdviceSchedule;
import com.screenleads.backend.app.domain.model.AdviceTimeWindow;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.Promotion;

import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.AdviceScheduleDTO;
import com.screenleads.backend.app.web.dto.AdviceTimeWindowDTO;
import com.screenleads.backend.app.web.dto.CompanyRefDTO;
import com.screenleads.backend.app.web.dto.MediaUpsertDTO;
import com.screenleads.backend.app.web.dto.PromotionRefDTO;

import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para Advice con el nuevo esquema:
 * - Advice.interval (Duration) <-> AdviceDTO.interval (Number: segundos)
 * - Advice.schedules[] (rango de fechas)
 * - AdviceTimeWindow[] (weekday + rangos horarios)
 *
 * Reglas:
 * - Si un día no tiene ventanas en un schedule, NO es visible ese día (lógica
 * se aplica en servicio).
 * - Horario [from, to) (fin exclusivo). Esta versión no soporta overnight en un
 * único rango.
 *
 * Además, incluye helpers de reflexión para tolerar distintos shapes en
 * Company/Promotion/Media DTOs.
 */
public final class AdviceMapper {

    private AdviceMapper() {
    }

    // ================= Entity -> DTO =================

    public static AdviceDTO toDto(Advice a) {
        if (a == null)
            return null;

        // interval Duration -> segundos
        Number intervalSecs = (a.getInterval() == null) ? null : a.getInterval().getSeconds();

        // media / promotion / company como DTOs "ref"
        MediaUpsertDTO mediaDto = (a.getMedia() != null)
                ? new MediaUpsertDTO(a.getMedia().getId(), a.getMedia().getSrc())
                : null;

        PromotionRefDTO promoDto = (a.getPromotion() != null)
                ? new PromotionRefDTO(a.getPromotion().getId())
                : null;

        CompanyRefDTO companyDto = (a.getCompany() != null)
                ? new CompanyRefDTO(a.getCompany().getId(), a.getCompany().getName())
                : null;

        // schedules -> DTO
        List<AdviceScheduleDTO> schedules = new ArrayList<>();
        if (a.getSchedules() != null) {
            for (AdviceSchedule s : a.getSchedules()) {
                List<AdviceTimeWindowDTO> wins = new ArrayList<>();
                if (s.getWindows() != null) {
                    for (AdviceTimeWindow w : s.getWindows()) {
                        wins.add(new AdviceTimeWindowDTO(
                                w.getId(),
                                w.getWeekday() != null ? w.getWeekday().name() : null,
                                formatTime(w.getFromTime()),
                                formatTime(w.getToTime())));
                    }
                }
                schedules.add(new AdviceScheduleDTO(
                        s.getId(),
                        formatDate(s.getStartDate()),
                        formatDate(s.getEndDate()),
                        wins,
                        null));
            }
        }

        return AdviceDTO.builder()
                .id(a.getId())
                .description(a.getDescription())
                .customInterval(Boolean.TRUE.equals(a.getCustomInterval()))
                .interval(intervalSecs)
                .media(mediaDto)
                .promotion(promoDto)
                .company(companyDto)
                .schedules(schedules)
                .build();
    }

    /** Alias: por si lo referenciabas como AdviceMapper::toDTO */
    public static AdviceDTO toDTO(Advice a) {
        return toDto(a);
    }

    // ================= DTO -> Entity =================

    public static Advice toEntity(AdviceDTO dto) {
        if (dto == null)
            return null;

        Advice a = new Advice();
        a.setId(dto.getId());
        a.setDescription(dto.getDescription());
        a.setCustomInterval(Boolean.TRUE.equals(dto.getCustomInterval()));
        a.setInterval(numberToDuration(dto.getInterval()));

        // refs
        a.setCompany(refToCompany(dto.getCompany()));
        a.setPromotion(refToPromotion(dto.getPromotion()));
        a.setMedia(refToMedia(dto.getMedia()));

        // schedules
        List<AdviceSchedule> schedules = new ArrayList<>();
        if (dto.getSchedules() != null) {
            for (AdviceScheduleDTO sDto : dto.getSchedules()) {
                AdviceSchedule s = new AdviceSchedule();
                s.setAdvice(a);
                s.setStartDate(parseDate(sDto.getStartDate()));
                s.setEndDate(parseDate(sDto.getEndDate()));

                List<AdviceTimeWindow> wins = new ArrayList<>();
                if (sDto.getWindows() != null) {
                    for (AdviceTimeWindowDTO wDto : sDto.getWindows()) {
                        AdviceTimeWindow w = new AdviceTimeWindow();
                        w.setSchedule(s);
                        w.setWeekday(parseWeekday(wDto.getWeekday()));
                        w.setFromTime(parseTime(wDto.getFromTime()));
                        w.setToTime(parseTime(wDto.getToTime()));
                        wins.add(w);
                    }
                }
                s.setWindows(wins);
                schedules.add(s);
            }
        }
        a.setSchedules(schedules);

        return a;
    }

    // ================= Helpers de referencia (con reflexión) =================

    private static Company refToCompany(Object companyDto) {
        if (companyDto == null)
            return null;
        Long id = extractLong(companyDto,
                "getId", "id", "getCompanyId", "companyId", "getCompanyID", "companyID");
        if (id == null)
            return null;
        Company c = new Company();
        c.setId(id);
        return c;
    }

    private static Promotion refToPromotion(Object promoDto) {
        if (promoDto == null)
            return null;
        Long id = extractLong(promoDto,
                "getId", "id", "getPromotionId", "promotionId", "getPromotionID", "promotionID");
        if (id == null)
            return null;
        Promotion p = new Promotion();
        p.setId(id);
        return p;
    }

    private static Media refToMedia(Object mediaDto) {
        if (mediaDto == null)
            return null;
        Long id = extractLong(mediaDto,
                "getId", "id", "getMediaId", "mediaId", "getMediaID", "mediaID");
        String src = extractString(mediaDto, "getSrc", "src");

        if (id == null && (src == null || src.isBlank()))
            return null;

        Media m = new Media();
        if (id != null)
            m.setId(id);
        if (src != null && !src.isBlank())
            m.setSrc(src.trim());
        return m;
    }

    // ================= Utilidades date/time =================

    private static Duration numberToDuration(Number n) {
        if (n == null)
            return null;
        long s = n.longValue();
        return (s > 0) ? Duration.ofSeconds(s) : null;
        // si s <= 0 devolvemos null para "no usar intervalo personalizado"
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank())
            return null;
        return LocalDate.parse(s.trim()); // "YYYY-MM-DD"
    }

    private static String formatDate(LocalDate d) {
        return (d == null) ? null : d.toString();
    }

    private static LocalTime parseTime(String s) {
        if (s == null || s.isBlank())
            return null;
        String t = s.trim();
        if (t.matches("^\\d{2}:\\d{2}$"))
            t = t + ":00";
        return LocalTime.parse(t, DateTimeFormatter.ISO_LOCAL_TIME); // "HH:mm[:ss]"
    }

    private static String formatTime(LocalTime t) {
        return (t == null) ? null : t.toString(); // HH:mm:ss
    }

    private static DayOfWeek parseWeekday(String s) {
        if (s == null || s.isBlank())
            return null;
        return DayOfWeek.valueOf(s.trim().toUpperCase());
    }

    // ================= Utilidades de reflexión seguras =================

    private static Long extractLong(Object obj, String... accessors) {
        for (String name : accessors) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object v = m.invoke(obj);
                if (v instanceof Number)
                    return ((Number) v).longValue();
                if (v != null && v.getClass() == Long.class)
                    return (Long) v;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String extractString(Object obj, String... accessors) {
        for (String name : accessors) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object v = m.invoke(obj);
                if (v instanceof String)
                    return (String) v;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/mapper/ApiKeyMapper.java
package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.web.dto.ApiKeyDTO;

public class ApiKeyMapper {

    public static ApiKeyDTO toDto(ApiKey apiKey) {
        if (apiKey == null) {
            return null;
        }

        ApiKeyDTO dto = new ApiKeyDTO();
        dto.setId(apiKey.getId());
        dto.setKey(apiKey.getKey());
        dto.setActive(apiKey.isActive());
        dto.setCreatedAt(apiKey.getCreatedAt());
        dto.setExpiresAt(apiKey.getExpiresAt());
        dto.setPermissions(apiKey.getPermissions());

        return dto;
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/mapper/AppEntityMapper.java
// src/main/java/com/screenleads/backend/app/web/mapper/AppEntityMapper.java
package com.screenleads.backend.app.web.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.AppEntityAttribute;
import com.screenleads.backend.app.web.dto.AppEntityDTO;
import com.screenleads.backend.app.web.dto.EntityAttributeDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AppEntityMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AppEntityMapper() {
    }

    // -------- AppEntity -> DTO --------
    public static AppEntityDTO toDto(AppEntity e) {
        List<EntityAttributeDTO> attrs = new ArrayList<>();
        if (e.getAttributes() != null) {
            e.getAttributes().stream()
                    .sorted(Comparator.comparing(a -> a.getListOrder() == null ? Integer.MAX_VALUE : a.getListOrder()))
                    .map(AppEntityMapper::toDto)
                    .forEach(attrs::add);
        }

        return AppEntityDTO.builder()
                .id(e.getId())
                .resource(e.getResource())
                .entityName(e.getEntityName())
                .className(e.getClassName())
                .tableName(e.getTableName())
                .idType(e.getIdType())
                .endpointBase(e.getEndpointBase())
                .createLevel(e.getCreateLevel())
                .readLevel(e.getReadLevel())
                .updateLevel(e.getUpdateLevel())
                .deleteLevel(e.getDeleteLevel())
                .visibleInMenu(e.getVisibleInMenu())
                .rowCount(e.getRowCount())
                .displayLabel(e.getDisplayLabel())
                .icon(e.getIcon())
                .sortOrder(e.getSortOrder())
                .attributes(attrs)
                .build();
    }

    // -------- AppEntityAttribute -> DTO --------
    public static EntityAttributeDTO toDto(AppEntityAttribute a) {
        String enumJson = null;
        if (a.getEnumValues() != null) {
            try {
                enumJson = MAPPER.writeValueAsString(a.getEnumValues());
            } catch (Exception ignore) {
            }
        }

        return EntityAttributeDTO.builder()
                .id(a.getId())
                .name(a.getName())
                .attrType(a.getAttrType())
                .dataType(a.getDataType())
                .relationTarget(a.getRelationTarget())
                .enumValuesJson(enumJson)

                .listVisible(a.getListVisible())
                .listOrder(a.getListOrder())
                .listLabel(a.getListLabel())
                .listWidthPx(a.getListWidthPx())
                .listAlign(a.getListAlign())
                .listSearchable(a.getListSearchable())
                .listSortable(a.getListSortable())

                .formVisible(a.getFormVisible())
                .formOrder(a.getFormOrder())
                .formLabel(a.getFormLabel())
                .controlType(a.getControlType())
                .placeholder(a.getPlaceholder())
                .helpText(a.getHelpText())
                .required(a.getRequired())
                .readOnly(a.getReadOnly())

                .minNum(a.getMinNum())
                .maxNum(a.getMaxNum())
                .minLen(a.getMinLen())
                .maxLen(a.getMaxLen())
                .pattern(a.getPattern())

                .defaultValue(a.getDefaultValue())
                .optionsEndpoint(a.getOptionsEndpoint())
                .build();
    }

    // -------- DTO -> AppEntityAttribute (aplicar valores no nulos) --------
    public static void applyAttrDto(AppEntityAttribute a, EntityAttributeDTO d) {
        if (d == null)
            return;

        if (d.name() != null)
            a.setName(d.name());
        if (d.attrType() != null)
            a.setAttrType(d.attrType());
        if (d.dataType() != null)
            a.setDataType(d.dataType());
        if (d.relationTarget() != null)
            a.setRelationTarget(d.relationTarget());

        // Preferir la lista nativa; si viene vacía pero hay enumValuesJson, parsear
        if (d.enumValuesJson() != null && !d.enumValuesJson().isBlank()) {
            try {
                List<String> parsed = MAPPER.readValue(d.enumValuesJson(), new TypeReference<List<String>>() {
                });
                a.setEnumValues(parsed);
            } catch (Exception ignore) {
            }
        }

        if (d.listVisible() != null)
            a.setListVisible(d.listVisible());
        if (d.listOrder() != null)
            a.setListOrder(d.listOrder());
        if (d.listLabel() != null)
            a.setListLabel(d.listLabel());
        if (d.listWidthPx() != null)
            a.setListWidthPx(d.listWidthPx());
        if (d.listAlign() != null)
            a.setListAlign(d.listAlign());
        if (d.listSearchable() != null)
            a.setListSearchable(d.listSearchable());
        if (d.listSortable() != null)
            a.setListSortable(d.listSortable());

        if (d.formVisible() != null)
            a.setFormVisible(d.formVisible());
        if (d.formOrder() != null)
            a.setFormOrder(d.formOrder());
        if (d.formLabel() != null)
            a.setFormLabel(d.formLabel());
        if (d.controlType() != null)
            a.setControlType(d.controlType());
        if (d.placeholder() != null)
            a.setPlaceholder(d.placeholder());
        if (d.helpText() != null)
            a.setHelpText(d.helpText());
        if (d.required() != null)
            a.setRequired(d.required());
        if (d.readOnly() != null)
            a.setReadOnly(d.readOnly());

        if (d.minNum() != null)
            a.setMinNum(d.minNum());
        if (d.maxNum() != null)
            a.setMaxNum(d.maxNum());
        if (d.minLen() != null)
            a.setMinLen(d.minLen());
        if (d.maxLen() != null)
            a.setMaxLen(d.maxLen());
        if (d.pattern() != null)
            a.setPattern(d.pattern());

        if (d.defaultValue() != null)
            a.setDefaultValue(d.defaultValue());
        if (d.optionsEndpoint() != null)
            a.setOptionsEndpoint(d.optionsEndpoint());
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/mapper/DeviceMapper.java
package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.web.dto.CompanyRefDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;
import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

public class DeviceMapper {
    public static DeviceDTO toDTO(Device device) {
        return new DeviceDTO(
            device.getId(),
            device.getUuid(),
            device.getDescriptionName(),
            device.getWidth(),
            device.getHeight(),
            toDeviceTypeDTO(device.getType()),
            toCompanyRefDTO(device.getCompany())
        );
    }

    public static DeviceTypeDTO toDeviceTypeDTO(DeviceType type) {
        if (type == null) return null;
        return new DeviceTypeDTO(type.getId(), type.getType(), type.getEnabled());
    }

    public static CompanyRefDTO toCompanyRefDTO(Company company) {
        if (company == null) return null;
        return new CompanyRefDTO(company.getId(), company.getName());
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/mapper/RoleMapper.java
// com.screenleads.backend.app.web.mapper.RoleMapper
package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.web.dto.RoleDTO;

public class RoleMapper {

    public static RoleDTO toDTO(Role r) {
        if (r == null)
            return null;
        return new RoleDTO(
                r.getId(), r.getRole(), r.getDescription(), r.getLevel());
    }

    public static Role toEntity(RoleDTO d) {
        if (d == null)
            return null;
        return Role.builder()
                .id(d.id())
                .role(d.role())
                .description(d.description())
                .level(d.level())
                .build();
    }
}

```

```java
// src/main/java/com/screenleads/backend/app/web/mapper/UserMapper.java
package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.web.dto.UserDto;


public class UserMapper {
    public static UserDto toDto(User u) {
        if (u == null)
            return null;
    return UserDto.builder()
        .id(u.getId())
        .username(u.getUsername())
        .email(u.getEmail())
        .name(u.getName())
        .lastName(u.getLastName())
        .companyId(u.getCompany() != null ? u.getCompany().getId() : null)
        .company(u.getCompany() != null ? new com.screenleads.backend.app.web.dto.CompanyRefDTO(u.getCompany().getId(), u.getCompany().getName()) : null)
        .profileImage(u.getProfileImage() != null ? new com.screenleads.backend.app.web.dto.MediaSlimDTO(
            u.getProfileImage().getId(),
            u.getProfileImage().getSrc(),
            u.getProfileImage().getType() != null ? new com.screenleads.backend.app.web.dto.MediaTypeDTO(
                u.getProfileImage().getType().getId(),
                u.getProfileImage().getType().getExtension(),
                u.getProfileImage().getType().getType(),
                u.getProfileImage().getType().getEnabled()
            ) : null,
            u.getProfileImage().getCreatedAt(),
            u.getProfileImage().getUpdatedAt()
        ) : null)
        .role(u.getRole() != null ? new com.screenleads.backend.app.web.dto.RoleDTO(
            u.getRole().getId(),
            u.getRole().getRole(),
            u.getRole().getDescription(),
            u.getRole().getLevel()
        ) : null)
        .build();
    }
}

```

