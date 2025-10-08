package com.screenleads.backend.app.domain.model;

import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_customer_company_identifier",
            columnNames = {"company_id", "identifier_type", "identifier"}
        )
    },
    indexes = {
        @Index(name = "ix_customer_company", columnList = "company_id"),
        @Index(name = "ix_customer_identifier", columnList = "identifier")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cliente pertenece a una empresa (la misma de la promoción)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_customer_company"))
    private Company company;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "identifier_type", nullable = false, length = 20)
    private LeadIdentifierType identifierType;

    @Column(name = "identifier", nullable = false, length = 255)
    private String identifier; // normalizado (email lowercase, phone E.164, etc.)

    @OneToMany(mappedBy = "customer")
    private Set<PromotionLead> leads;
}
