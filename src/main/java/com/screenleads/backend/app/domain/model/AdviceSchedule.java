package com.screenleads.backend.app.domain.model;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdviceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Rango de fechas (inclusive). Si son null → sin límite por ese lado. */
    private LocalDate startDate; // nullable
    private LocalDate endDate;   // nullable

    @ManyToOne
    @JoinColumn(name = "advice_id")
    @JsonIgnore
    private Advice advice;

    /** Ventanas por día (múltiples por día). */
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdviceTimeWindow> windows;
}
