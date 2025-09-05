package com.screenleads.backend.app.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.screenleads.backend.app.domain.model.AdviceVisibilityRule;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.Promotion;
import com.screenleads.backend.app.web.json.MediaIdOrNullDeserializer;
import com.screenleads.backend.app.web.json.PromotionIdOrNullDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true) // Ignora "company" y cualquier otro campo extra
public record AdviceDTO(
                Long id,
                String description,
                Boolean customInterval,
                Number interval,
                @JsonDeserialize(using = MediaIdOrNullDeserializer.class) Media media,
                @JsonDeserialize(using = PromotionIdOrNullDeserializer.class) Promotion promotion,
                List<AdviceVisibilityRule> visibilityRules) {
}
