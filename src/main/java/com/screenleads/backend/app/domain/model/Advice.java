package com.screenleads.backend.app.domain.model;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Filter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder(toBuilder = true)
@Table(uniqueConstraints = {})
@Setter @Getter
@NoArgsConstructor @AllArgsConstructor
@Filter(name = "companyFilter", condition = "company_id = :companyId")
public class Advice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private Boolean customInterval;

    @Column(name = "interval_seconds")
    private Duration interval; // segundos (Duration)

    @ManyToOne
    @JoinColumn(name = "company_id", referencedColumnName = "id")
    @JsonIgnore
    private Company company;

    @ManyToOne
    @JoinColumn(name = "media", referencedColumnName = "id")
    private Media media;

    @ManyToOne
    @JoinColumn(name = "promotion", referencedColumnName = "id")
    private Promotion promotion;

    /** Múltiples rangos de fechas, cada uno con ventanas por día. */
    @OneToMany(mappedBy = "advice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdviceSchedule> schedules;

    @JsonIgnore
    @ManyToMany(mappedBy = "advices")
    private Set<Device> devices;
}
