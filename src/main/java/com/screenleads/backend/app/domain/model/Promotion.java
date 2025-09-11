package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder(toBuilder = true)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String legal_url;
    private String url;
    private String description;

    @Column(columnDefinition = "TEXT") // permite HTML largo
    private String templateHtml;

    // === Reglas de lead ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadLimitType leadLimitType = LeadLimitType.NO_LIMIT; // NO_LIMIT | ONE_PER_24H | ONE_PER_PERSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadIdentifierType leadIdentifierType = LeadIdentifierType.EMAIL; // EMAIL | PHONE

    // Relación con leads (carga LAZY)
    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PromotionLead> leads = new ArrayList<>();
}
