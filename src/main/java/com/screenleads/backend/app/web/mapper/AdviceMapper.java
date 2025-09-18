package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.Promotion;
import com.screenleads.backend.app.web.dto.AdviceDTO;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper tolerante a distintos shapes de DTO:
 * - AdviceDTO (Lombok @Data/@Builder) que conoces.
 * - CompanyRefDTO/PromotionRefDTO/MediaUpsertDTO pueden ser record, @Data con otros nombres (companyId, mediaId...), etc.
 *   Se leen con reflexión (getId(), id(), getCompanyId(), companyId(), ...).
 */
public final class AdviceMapper {

    private AdviceMapper() {}

    // ================= Entity -> DTO =================

    public static AdviceDTO toDto(Advice a) {
        if (a == null) return null;

        return AdviceDTO.builder()
                .id(a.getId())
                .description(a.getDescription())
                .customInterval(Boolean.TRUE.equals(a.getCustomInterval()))
                // AdviceDTO.interval es Number; entity usa Integer intervalSeconds
                .interval(a.getIntervalSeconds())
                // Para evitar dependencias de constructores/builder desconocidos, dejamos refs en null.
                // Si necesitas hidratar estos sub-DTOs, dímelos y lo añadimos exacto.
                .media(null)
                .promotion(null)
                .company(null)
                // Tu AdviceDTO expone directamente List<AdviceVisibilityRule>:
                .visibilityRules(a.getVisibilityRules() != null ? a.getVisibilityRules() : List.of())
                .build();
    }

    /** Alias por si lo usas como referencia de método: AdviceMapper::toDTO */
    public static AdviceDTO toDTO(Advice a) { return toDto(a); }

    // ================= DTO -> Entity =================

    public static Advice toEntity(AdviceDTO dto) {
        if (dto == null) return null;

        Advice a = Advice.builder()
                .id(dto.getId())
                .description(dto.getDescription())
                .customInterval(dto.getCustomInterval())
                .intervalSeconds(dto.getInterval() != null ? dto.getInterval().intValue() : null)
                .build();

        // Company
        a.setCompany(refToCompany(dto.getCompany()));
        // Promotion
        a.setPromotion(refToPromotion(dto.getPromotion()));
        // Media
        a.setMedia(refToMedia(dto.getMedia()));

        // Reglas (ya vienen como entidades en el DTO)
        a.setVisibilityRules(dto.getVisibilityRules() != null ? dto.getVisibilityRules() : new ArrayList<>());

        return a;
    }

    // ================= Helpers de referencia (con reflexión) =================

    private static Company refToCompany(Object companyDto) {
        if (companyDto == null) return null;
        Long id = extractLong(companyDto,
                "getId", "id", "getCompanyId", "companyId", "getCompanyID", "companyID");
        if (id == null) return null;
        Company c = new Company();
        c.setId(id);
        return c;
    }

    private static Promotion refToPromotion(Object promoDto) {
        if (promoDto == null) return null;
        Long id = extractLong(promoDto,
                "getId", "id", "getPromotionId", "promotionId", "getPromotionID", "promotionID");
        if (id == null) return null;
        Promotion p = new Promotion();
        p.setId(id);
        return p;
    }

    private static Media refToMedia(Object mediaDto) {
        if (mediaDto == null) return null;
        Long id = extractLong(mediaDto,
                "getId", "id", "getMediaId", "mediaId", "getMediaID", "mediaID");
        Media m = new Media();
        if (id != null) m.setId(id);

        // Si el DTO tiene src, lo copiamos (si no, simplemente se ignora)
        String src = extractString(mediaDto, "getSrc", "src");
        if (src != null) m.setSrc(src);

        return m.getId() == null && m.getSrc() == null ? null : m;
    }

    // ================= Utilidades de reflexión seguras =================

    private static Long extractLong(Object obj, String... accessors) {
        for (String name : accessors) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object v = m.invoke(obj);
                if (v instanceof Number) return ((Number) v).longValue();
                // Para records con id() que devuelve Long correctamente tipado
                if (v != null && v.getClass() == Long.class) return (Long) v;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                // ignora y prueba siguiente
            }
        }
        return null;
    }

    private static String extractString(Object obj, String... accessors) {
        for (String name : accessors) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object v = m.invoke(obj);
                if (v instanceof String) return (String) v;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                // ignora y prueba siguiente
            }
        }
        return null;
    }
}
