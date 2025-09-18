package com.screenleads.backend.app.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(
  name = "time_range",
  indexes = {
    @Index(name="idx_timerange_rule", columnList="rule_id")
  }
)
@ToString(onlyExplicitlyIncluded = true)
public class TimeRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="from_time", nullable = false)
    @ToString.Include
    private LocalTime fromTime;

    @Column(name="to_time", nullable = false)
    @ToString.Include
    private LocalTime toTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    @JsonIgnore
    private AdviceVisibilityRule rule;
}
