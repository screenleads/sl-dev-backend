package com.screenleads.backend.app.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.screenleads.backend.app.domain.model.AdviceVisibilityRule;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.Promotion;
import com.screenleads.backend.app.web.json.MediaIdOrNullDeserializer;
import com.screenleads.backend.app.web.json.PromotionIdOrNullDeserializer;
import java.util.List;
import com.screenleads.backend.app.domain.model.AdviceVisibilityRule;

public record AdviceDTO(
                Long id,
                String description,
                Boolean customInterval,
                Number interval,
                MediaUpsertDTO media, // acepta {id} o {src}
                PromotionRefDTO promotion, // referencia por id
                List<AdviceVisibilityRule> visibilityRules,
                CompanyRefDTO company // NUEVO: compañía (id)
) {
}
