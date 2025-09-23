package com.screenleads.backend.app.domain.model;


import java.time.ZonedDateTime;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "promotion_lead",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_promotionlead_promotion_identifier",
    columnNames = {"promotion_id", "identifier"}
  ),
  indexes = {
    @Index(name = "ix_promotionlead_promotion", columnList = "promotion_id"),
    @Index(name = "ix_promotionlead_created_at", columnList = "created_at")
  }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromotionLead extends Auditable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "promotion_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_promotionlead_promotion"))
  private Promotion promotion;

  @Enumerated(EnumType.STRING)
  @Column(name = "identifier_type", nullable = false, length = 20)
  private LeadIdentifierType identifierType;

  @Column(nullable = false, length = 255)
  private String identifier;

  @Enumerated(EnumType.STRING)
  @Column(name = "limit_type", length = 20)
  private LeadLimitType limitType;
}
