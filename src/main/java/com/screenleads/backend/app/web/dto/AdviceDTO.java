package com.screenleads.backend.app.web.dto;

import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.Promotion;

public record AdviceDTO(Long id, String description, Boolean customInterval, Number interval,
                Media media, Promotion promotion) {
}
