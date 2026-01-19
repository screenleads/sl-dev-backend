package com.screenleads.backend.app.domain.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.screenleads.backend.app.domain.model.Invoice;
import com.screenleads.backend.app.domain.model.InvoiceStatus;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId);

    List<Invoice> findByCompanyBillingId(Long companyBillingId);

    List<Invoice> findByStatus(InvoiceStatus status);

    @Query("SELECT i FROM Invoice i WHERE i.companyBilling.id = :companyBillingId ORDER BY i.issueDate DESC")
    List<Invoice> findByCompanyBillingIdOrderByIssueDateDesc(@Param("companyBillingId") Long companyBillingId);

    @Query("SELECT i FROM Invoice i WHERE i.companyBilling.id = :companyBillingId AND i.periodStart = :periodStart AND i.periodEnd = :periodEnd")
    Optional<Invoice> findByCompanyBillingAndPeriod(
            @Param("companyBillingId") Long companyBillingId,
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd);

    @Query("SELECT i FROM Invoice i WHERE i.status = :status AND i.dueDate < :now")
    List<Invoice> findOverdueInvoices(@Param("status") InvoiceStatus status, @Param("now") Instant now);
}
