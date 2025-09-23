package com.screenleads.backend.app.domain.model;

import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "promotion",
    indexes = {
        @Index(name = "ix_promotion_company", columnList = "company_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    // ✅ camelCase en el modelo; columna en snake_case
    @Column(name = "legal_url", length = 2048)
    private String legalUrl;

    @Column(length = 2048)
    private String url;

    @Lob
    @Column(name = "template_html")
    private String templateHtml;

    // ✅ NUEVO: tipos de identificación y límite de leads
    @Enumerated(EnumType.STRING)
    @Column(name = "lead_identifier_type", length = 30, nullable = false)
    @Builder.Default
    private LeadIdentifierType leadIdentifierType = LeadIdentifierType.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_limit_type", length = 30, nullable = false)
    @Builder.Default
    private LeadLimitType leadLimitType = LeadLimitType.NO_LIMIT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "company_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_promotion_company")
    )
    private Company company;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PromotionLead> leads;
}
