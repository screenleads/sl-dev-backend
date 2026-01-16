package com.screenleads.backend.app.domain.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.screenleads.backend.app.domain.model.BillingEvent;
import com.screenleads.backend.app.domain.model.enums.BillingEventStatus;
import com.screenleads.backend.app.domain.model.enums.BillingEventType;

public interface BillingEventRepository extends JpaRepository<BillingEvent, Long> {
    
    Optional<BillingEvent> findByStripeEventId(String stripeEventId);
    
    List<BillingEvent> findByCompanyBillingId(Long companyBillingId);
    
    List<BillingEvent> findByRedemptionId(Long redemptionId);
    
    List<BillingEvent> findByInvoiceId(Long invoiceId);
    
    List<BillingEvent> findByEventType(BillingEventType eventType);
    
    List<BillingEvent> findByStatus(BillingEventStatus status);
    
    @Query("SELECT be FROM BillingEvent be WHERE be.companyBilling.id = :companyBillingId ORDER BY be.timestamp DESC")
    List<BillingEvent> findByCompanyBillingIdOrderByTimestampDesc(@Param("companyBillingId") Long companyBillingId);
    
    @Query("SELECT be FROM BillingEvent be WHERE be.companyBilling.id = :companyBillingId AND be.timestamp BETWEEN :startDate AND :endDate")
    List<BillingEvent> findByCompanyBillingAndDateRange(
        @Param("companyBillingId") Long companyBillingId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );
    
    @Query("SELECT be FROM BillingEvent be WHERE be.status = :status AND be.eventType = :eventType")
    List<BillingEvent> findByStatusAndEventType(@Param("status") BillingEventStatus status, @Param("eventType") BillingEventType eventType);
}
