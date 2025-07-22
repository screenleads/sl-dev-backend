package com.screenleads.backend.app.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder(toBuilder = true)
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "uuid" }) })
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String uuid;
    private String descriptionName;
    private Number width;
    private Number height;

    @ManyToOne
    @JoinColumn(name = "type", referencedColumnName = "id")
    private DeviceType type;

    @ManyToOne
    @JoinColumn(name = "company_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"users", "devices", "advices"}) 
    private Company company;

    @ManyToMany
    @JoinTable(name = "device_advice", joinColumns = @JoinColumn(name = "device_id"), inverseJoinColumns = @JoinColumn(name = "advice_id"))
    private Set<Advice> advices;
}
