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
                if (v instanceof Number n)
                    return n.longValue();
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
                if (v instanceof String s)
                    return s;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
