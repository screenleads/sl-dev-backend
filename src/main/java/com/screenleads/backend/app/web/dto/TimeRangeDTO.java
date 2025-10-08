package com.screenleads.backend.app.web.dto;

import java.time.LocalTime;

public record TimeRangeDTO(
  Long id,
  LocalTime from,
  LocalTime to
) {}
