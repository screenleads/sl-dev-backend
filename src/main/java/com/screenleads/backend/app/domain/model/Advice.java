package com.screenleads.backend.app.domain.model;


import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.Filter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(
name = "advice",
indexes = {
@Index(name = "ix_advice_company", columnList = "company_id"),
@Index(name = "ix_advice_media", columnList = "media_id"),
@Index(name = "ix_advice_promotion", columnList = "promotion_id")
}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Filter(name = "companyFilter", condition = "company_id = :companyId")
public class Advice extends Auditable {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(length = 512)
private String description;


@Column(name = "custom_interval", nullable = false)
@Builder.Default
private Boolean customInterval = Boolean.FALSE;


/** Almacenada como segundos vía DurationToLongConverter */
@Column(name = "interval_seconds")
private Duration interval;


@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "company_id", nullable = false,
foreignKey = @ForeignKey(name = "fk_advice_company"))
@JsonIgnore
private Company company;


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "media_id",
foreignKey = @ForeignKey(name = "fk_advice_media"))
private Media media;


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "promotion_id",
foreignKey = @ForeignKey(name = "fk_advice_promotion"))
private Promotion promotion;


/** Rango(s) de fechas y ventanas por día */
@OneToMany(mappedBy = "advice", cascade = CascadeType.ALL, orphanRemoval = true)
private List<AdviceSchedule> schedules;


@ManyToMany(mappedBy = "advices")
@JsonIgnore
@Builder.Default
private Set<Device> devices = new HashSet<>();
}