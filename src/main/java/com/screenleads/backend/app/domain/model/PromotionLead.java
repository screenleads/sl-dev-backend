package com.screenleads.backend.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "promotion_lead", uniqueConstraints = {
        @UniqueConstraint(name = "uk_promotion_identifier", columnNames = { "promotion_id", "identifier" })
}, indexes = {
        @Index(name = "idx_promotion_created_at", columnList = "promotion_id, createdAt")
})
@Setter
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PromotionLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    // Identificador normalizado (email en lower-case o teléfono normalizado)
    @Column(nullable = false, length = 255)
    private String identifier;

    private String firstName;
    private String lastName;

    @Column(length = 320)
    private String email;

    @Column(length = 32)
    private String phone;

    private LocalDate birthDate;

    private ZonedDateTime acceptedPrivacyAt;
    private ZonedDateTime acceptedTermsAt;

    @Column(nullable = false)
    private ZonedDateTime createdAt;
}
