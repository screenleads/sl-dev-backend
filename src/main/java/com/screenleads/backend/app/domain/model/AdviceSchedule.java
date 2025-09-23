package com.screenleads.backend.app.domain.model;


import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "advice_schedule",
indexes = {
@Index(name = "ix_adviceschedule_dates", columnList = "start_date,end_date"),
@Index(name = "ix_adviceschedule_advice", columnList = "advice_id")
}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdviceSchedule extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(name = "start_date")
private LocalDate startDate; // nullable → sin límite inferior


@Column(name = "end_date")
private LocalDate endDate; // nullable → sin límite superior


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "advice_id",
foreignKey = @ForeignKey(name = "fk_adviceschedule_advice"))
@JsonIgnore
private Advice advice;


@OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
private List<AdviceTimeWindow> windows;
}