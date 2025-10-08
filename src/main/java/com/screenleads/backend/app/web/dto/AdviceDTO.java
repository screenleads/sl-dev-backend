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
