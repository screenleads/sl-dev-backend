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
@Table(
  name = "advice",
  indexes = {
    @Index(name="idx_advice_company", columnList="company_id"),
    @Index(name="idx_advice_media", columnList="media"),
    @Index(name="idx_advice_promotion", columnList="promotion")
  }
)
@Setter @Getter
@NoArgsConstructor @AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Filter(name = "companyFilter", condition = "company_id = :companyId")
public class Advice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Include
    private String description;

    // Si prefieres minutos: cambia a Integer minutos y documenta la unidad
    @Column(name = "custom_interval", nullable = true)
    private Boolean customInterval;

    // Almacena en segundos (recomendado) si usas Integer; si usas Duration, mapea con AttributeConverter.
    @Column(name = "interval_seconds", nullable = true)
    private Integer intervalSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", referencedColumnName = "id")
    @JsonIgnore
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media", referencedColumnName = "id")
    private Media media;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion", referencedColumnName = "id")
    private Promotion promotion;

    @OneToMany(mappedBy = "advice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("day ASC, startDate NULLS FIRST, endDate NULLS FIRST, id ASC")
    private List<AdviceVisibilityRule> visibilityRules;

    @JsonIgnore
    @ManyToMany(mappedBy = "advices", fetch = FetchType.LAZY)
    private Set<Device> devices;

    /** Helpers */
    public void setVisibilityRules(List<AdviceVisibilityRule> rules) {
        this.visibilityRules = rules;
        if (rules != null) {
            rules.forEach(r -> r.setAdvice(this));
        }
    }

    /** Si quieres exponer Duration en dominio: */
    public Duration getInterval() {
        return intervalSeconds == null ? null : Duration.ofSeconds(intervalSeconds);
    }
    public void setInterval(Duration d) {
        this.intervalSeconds = (d == null) ? null : Math.toIntExact(d.getSeconds());
    }
}
