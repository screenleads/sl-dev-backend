package com.screenleads.backend.app.web.dto;

import java.time.DayOfWeek;
import java.util.List;

import com.screenleads.backend.app.domain.model.TimeRange;

public record AdviceVisibilityRuleDTO(Long id, DayOfWeek day, List<TimeRange> timeRanges) {
}
