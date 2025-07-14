package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.web.dto.AdviceDTO;

public class AdviceMapper {
    public static AdviceDTO toDTO(Advice advice) {
        return new AdviceDTO(
                advice.getId(),
                advice.getDescription(),
                advice.getCustomInterval(),
                advice.getInterval(),
                advice.getMedia(),
                advice.getPromotion(),
                advice.getVisibilityRules());
    }
}
