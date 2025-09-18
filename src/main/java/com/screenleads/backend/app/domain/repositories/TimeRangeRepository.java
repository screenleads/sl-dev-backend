package com.screenleads.backend.app.domain.repositories;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.screenleads.backend.app.domain.model.TimeRange;

public interface TimeRangeRepository extends JpaRepository<TimeRange, Long> {
    @Query("""
        SELECT (COUNT(tr.id) > 0)
        FROM Advice a
          JOIN a.visibilityRules r
          JOIN r.timeRanges tr
        WHERE a.id = :adviceId
          AND r.day = :dow
          AND (:d >= r.startDate OR r.startDate IS NULL)
          AND (:d <= r.endDate   OR r.endDate   IS NULL)
          AND :t >= tr.fromTime
          AND :t <  tr.toTime
    """)
    boolean existsActive(Long adviceId, DayOfWeek dow, LocalDate d, LocalTime t);
}