package com.screenleads.backend.app.domain.model;

import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Filter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder(toBuilder = true)
@Table(uniqueConstraints = {})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "companyFilter", condition = "company_id = :companyId")
public class Advice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private Boolean customInterval;
    private Number interval;

    @ManyToOne
    @JoinColumn(name = "company_id", referencedColumnName = "id") // <-- clave: coincide con el @Filter
    @JsonIgnore
    private Company company;

    @ManyToOne
    @JoinColumn(name = "media", referencedColumnName = "id")
    private Media media;

    @ManyToOne
    @JoinColumn(name = "promotion", referencedColumnName = "id")
    private Promotion promotion;

    @OneToMany(mappedBy = "advice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdviceVisibilityRule> visibilityRules;

    @JsonIgnore
    @ManyToMany(mappedBy = "advices")
    private Set<Device> devices;
}
