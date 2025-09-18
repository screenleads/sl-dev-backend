package com.screenleads.backend.app.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdviceTimeWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DayOfWeek weekday;

    /** Intervalo [from,to) (fin exclusivo). En esta versión no cruza medianoche. */
    private LocalTime fromTime;
    private LocalTime toTime;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    @JsonIgnore
    private AdviceSchedule schedule;
}
