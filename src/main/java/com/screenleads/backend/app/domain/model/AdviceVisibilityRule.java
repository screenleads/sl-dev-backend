package com.screenleads.backend.app.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(
  name = "advice_visibility_rule",
  indexes = {
    @Index(name="idx_rule_advice", columnList="advice_id"),
    @Index(name="idx_rule_day", columnList="day"),
    @Index(name="idx_rule_window", columnList="start_date,end_date")
  }
  // Si NO quieres duplicados exactos, añade uniqueConstraints aquí
)
@ToString(onlyExplicitlyIncluded = true)
public class AdviceVisibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @ToString.Include
    private DayOfWeek day;

    // Ventana opcional (inclusive). Si es null, no limita por fecha.
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // (Opcional) prioridad para resolver solapes de reglas
    @Column(name = "priority")
    private Integer priority;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("fromTime ASC, toTime ASC, id ASC")
    private List<TimeRange> timeRanges;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advice_id", nullable = false)
    @JsonIgnore
    private Advice advice;

    public void setTimeRanges(List<TimeRange> ranges) {
        this.timeRanges = ranges;
        if (ranges != null) {
            ranges.forEach(tr -> tr.setRule(this));
        }
    }
}
