package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdviceVisibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DayOfWeek day;

    // NUEVO: rango de fechas opcional (inclusive). Si es null, no limita por fecha.
    private LocalDate startDate; // nullable
    private LocalDate endDate; // nullable

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeRange> timeRanges;

    @ManyToOne
    @JoinColumn(name = "advice_id")
    @JsonIgnore
    private Advice advice;
}
