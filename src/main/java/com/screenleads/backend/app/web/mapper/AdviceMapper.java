
package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.AdviceVisibilityRuleDTO;
import com.screenleads.backend.app.web.dto.TimeRangeDTO;

import java.util.List;
import java.util.Objects;

public final class AdviceMapper {

    private AdviceMapper() {}

    public static AdviceDTO toDto(Advice a) {
        return new AdviceDTO(
            a.getId(),
            a.getDescription(),
            a.getCustomInterval(),
            a.getIntervalSeconds(),
            a.getCompany() != null ? a.getCompany().getId() : null,
            a.getMedia() != null ? a.getMedia().getId() : null,
            a.getPromotion() != null ? a.getPromotion().getId() : null,
            a.getVisibilityRules() == null ? List.of() :
                a.getVisibilityRules().stream().map(AdviceMapper::toDto).toList()
        );
    }

    public static AdviceVisibilityRuleDTO toDto(AdviceVisibilityRule r) {
        return new AdviceVisibilityRuleDTO(
            r.getId(),
            r.getDay(),
            r.getStartDate(),
            r.getEndDate(),
            r.getPriority(),
            r.getTimeRanges() == null ? List.of() :
                r.getTimeRanges().stream().map(AdviceMapper::toDto).toList()
        );
    }

    public static TimeRangeDTO toDto(TimeRange tr) {
        return new TimeRangeDTO(tr.getId(), tr.getFromTime(), tr.getToTime());
    }

    /** Construye Advice desde DTO. company/media/promotion se inyectan fuera. */
    public static Advice toEntity(AdviceDTO dto, Company company, Media media, Promotion promo) {
        Advice a = Advice.builder()
                .id(dto.id())
                .description(dto.description())
                .customInterval(dto.customInterval())
                .intervalSeconds(dto.intervalSeconds())
                .company(company)
                .media(media)
                .promotion(promo)
                .build();
        if (dto.rules() != null) {
            a.setVisibilityRules(dto.rules().stream().map(AdviceMapper::toEntity).toList());
        }
        return a;
    }

    public static AdviceVisibilityRule toEntity(AdviceVisibilityRuleDTO dto) {
        AdviceVisibilityRule r = AdviceVisibilityRule.builder()
                .id(dto.id())
                .day(Objects.requireNonNull(dto.day(), "day is required"))
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .priority(dto.priority())
                .build();
        if (dto.ranges() != null) {
            r.setTimeRanges(dto.ranges().stream().map(AdviceMapper::toEntity).toList());
        }
        return r;
    }

    public static TimeRange toEntity(TimeRangeDTO dto) {
        return TimeRange.builder()
                .id(dto.id())
                .fromTime(dto.from())
                .toTime(dto.to())
                .build();
    }
}
