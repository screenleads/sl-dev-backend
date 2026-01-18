
package com.screenleads.backend.app.domain.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Table(name = "company", indexes = {
        @Index(name = "ix_company_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends Auditable {
    public enum BillingStatus {
        INCOMPLETE,
        ACTIVE,
        PAST_DUE,
        CANCELED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "observations", length = 1000)
    private String observations;

    @Column(name = "primary_color", length = 7)
    private String primaryColor; // ej: #FFFFFF

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_id", foreignKey = @ForeignKey(name = "fk_company_logo"))
    private Media logo;

    @OneToMany(mappedBy = "company")
    @JsonIgnore
    private List<Device> devices;

    @OneToMany(mappedBy = "company")
    @JsonIgnore
    private List<Advice> advices;

    @OneToMany(mappedBy = "company")
    @JsonIgnore
    private List<User> users;

    // Stripe & Billing fields
    @Column(name = "stripe_customer_id", length = 64)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 64)
    private String stripeSubscriptionId;

    @Column(name = "stripe_subscription_item_id", length = 64)
    private String stripeSubscriptionItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_status", length = 32)
    @Builder.Default
    private BillingStatus billingStatus = BillingStatus.INCOMPLETE;

    public void setBillingStatus(String status) {
        this.billingStatus = BillingStatus.valueOf(status);
    }
}