package com.screenleads.backend.app.web.dto;

import java.util.List;

import com.screenleads.backend.app.domain.model.AdviceVisibilityRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO compatible con getters, setters y builder */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdviceDTO {
    private Long id;
    private String description;
    private Boolean customInterval;
    private Number interval; // usa Number/Integer según tu entidad Advice
    private MediaUpsertDTO media;
    private PromotionRefDTO promotion;
    private List<AdviceVisibilityRule> visibilityRules; // tal como ya usas en el servicio
    private CompanyRefDTO company;
}
