package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.TimeRange;

public interface TimeRangeRepository extends JpaRepository<TimeRange, Long> {
}