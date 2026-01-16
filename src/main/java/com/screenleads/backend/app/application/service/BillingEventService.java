package com.screenleads.backend.app.application.service;

import java.util.List;

import com.screenleads.backend.app.domain.model.BillingEventType;
import com.screenleads.backend.app.web.dto.BillingEventDTO;

public interface BillingEventService {
    
    List<BillingEventDTO> getAllEvents();
    
    List<BillingEventDTO> getEventsByCompanyBilling(Long companyBillingId);
    
    List<BillingEventDTO> getEventsByRedemption(Long redemptionId);
    
    List<BillingEventDTO> getEventsByInvoice(Long invoiceId);
    
    List<BillingEventDTO> getEventsByType(BillingEventType eventType);
    
    BillingEventDTO createEvent(BillingEventDTO dto);
    
    void trackLeadRegistered(Long companyBillingId, Long redemptionId, Integer quantity);
    
    void trackUsageReported(Long companyBillingId, String stripeUsageRecordId, Integer quantity);
    
    void trackInvoiceCreated(Long companyBillingId, Long invoiceId);
    
    void trackPaymentSucceeded(Long companyBillingId, Long invoiceId);
    
    void trackPaymentFailed(Long companyBillingId, Long invoiceId, String errorMessage);
}
