package com.screenleads.backend.app.web.dto;

import java.util.List;

import com.screenleads.backend.app.domain.model.AdviceVisibilityRule;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.Promotion;

public record AdviceDTO(Long id, String description, Boolean customInterval, Number interval,
        Media media, Promotion promotion, List<AdviceVisibilityRule> visibilityRules) {
}
