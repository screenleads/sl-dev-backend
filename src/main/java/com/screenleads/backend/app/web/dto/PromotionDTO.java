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
