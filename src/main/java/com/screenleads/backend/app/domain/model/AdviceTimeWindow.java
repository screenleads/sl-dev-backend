package com.screenleads.backend.app.domain.model;


import java.time.DayOfWeek;
import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "advice_time_window",
indexes = @Index(name = "ix_advicetimewindow_schedule", columnList = "schedule_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdviceTimeWindow extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 10)
private DayOfWeek weekday;


/** Intervalo [from,to) (fin exclusivo) */
@Column(name = "from_time", nullable = false)
private LocalTime fromTime;


@Column(name = "to_time", nullable = false)
private LocalTime toTime;


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "schedule_id",
foreignKey = @ForeignKey(name = "fk_advicetimewindow_schedule"))
@JsonIgnore
private AdviceSchedule schedule;
}