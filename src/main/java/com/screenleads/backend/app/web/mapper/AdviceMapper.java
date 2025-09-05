package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.MediaUpsertDTO;
import com.screenleads.backend.app.web.dto.PromotionRefDTO;
import com.screenleads.backend.app.web.dto.CompanyRefDTO;

public final class AdviceMapper {

    private AdviceMapper() {
    }

    public static AdviceDTO toDTO(Advice advice) {
        if (advice == null)
            return null;

        MediaUpsertDTO mediaDto = null;
        if (advice.getMedia() != null) {
            mediaDto = new MediaUpsertDTO(
                    advice.getMedia().getId(),
                    advice.getMedia().getSrc());
        }

        PromotionRefDTO promoDto = null;
        if (advice.getPromotion() != null) {
            promoDto = new PromotionRefDTO(advice.getPromotion().getId());
        }

        CompanyRefDTO companyDto = null;
        if (advice.getCompany() != null) {
            companyDto = new CompanyRefDTO(
                    advice.getCompany().getId(),
                    advice.getCompany().getName());
        }

        return new AdviceDTO(
                advice.getId(),
                advice.getDescription(),
                advice.getCustomInterval(),
                advice.getInterval(),
                mediaDto,
                promoDto,
                advice.getVisibilityRules(),
                companyDto);
    }
}
