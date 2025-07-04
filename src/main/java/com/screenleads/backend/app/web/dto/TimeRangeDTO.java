package com.screenleads.backend.app.web.dto;

import java.time.LocalTime;

import com.screenleads.backend.app.domain.model.AdviceVisibilityRule;

public record TimeRangeDTO(Long id, LocalTime fromTime, LocalTime toTime, AdviceVisibilityRule rule) {
}
