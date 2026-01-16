package com.screenleads.backend.app.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.InvoiceDTO;

public interface InvoiceService {
    
    List<InvoiceDTO> getAllInvoices();
    
    Optional<InvoiceDTO> getInvoiceById(Long id);
    
    Optional<InvoiceDTO> getInvoiceByNumber(String invoiceNumber);
    
    List<InvoiceDTO> getInvoicesByCompanyBilling(Long companyBillingId);
    
    InvoiceDTO createInvoice(InvoiceDTO dto);
    
    InvoiceDTO updateInvoice(Long id, InvoiceDTO dto);
    
    void deleteInvoice(Long id);
    
    InvoiceDTO finalizeInvoice(Long id);
    
    InvoiceDTO markAsPaid(Long id);
    
    List<InvoiceDTO> getOverdueInvoices();
    
    InvoiceDTO generateMonthlyInvoice(Long companyBillingId, Instant periodStart, Instant periodEnd);
}
