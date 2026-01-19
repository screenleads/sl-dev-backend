package com.screenleads.backend.app.domain.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.screenleads.backend.app.domain.model.CompanyBilling;
import com.screenleads.backend.app.domain.model.BillingStatus;

public interface CompanyBillingRepository extends JpaRepository<CompanyBilling, Long> {

    Optional<CompanyBilling> findByCompanyId(Long companyId);

    Optional<CompanyBilling> findByStripeCustomerId(String stripeCustomerId);

    Optional<CompanyBilling> findByStripeSubscriptionId(String stripeSubscriptionId);

    List<CompanyBilling> findByBillingStatus(BillingStatus status);

    List<CompanyBilling> findByActive(Boolean active);

    @Query("SELECT cb FROM CompanyBilling cb WHERE cb.billingStatus = :status AND cb.active = true")
    List<CompanyBilling> findActiveByBillingStatus(@Param("status") BillingStatus status);

    @Query("SELECT cb FROM CompanyBilling cb WHERE cb.currentPeriodLeads >= cb.monthlyLeadQuota AND cb.active = true")
    List<CompanyBilling> findCompaniesExceedingQuota();
}
