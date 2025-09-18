package com.screenleads.backend.app.web.dto;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record AdviceVisibilityRuleDTO(
  Long id,
  DayOfWeek day,
  LocalDate startDate,
  LocalDate endDate,
  Integer priority,
  List<TimeRangeDTO> ranges
) {}
