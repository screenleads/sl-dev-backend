package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.CompanyBillingDTO;

public interface CompanyBillingService {
    
    List<CompanyBillingDTO> getAllCompanyBillings();
    
    Optional<CompanyBillingDTO> getCompanyBillingById(Long id);
    
    Optional<CompanyBillingDTO> getCompanyBillingByCompanyId(Long companyId);
    
    Optional<CompanyBillingDTO> getCompanyBillingByStripeCustomerId(String stripeCustomerId);
    
    CompanyBillingDTO createCompanyBilling(CompanyBillingDTO dto);
    
    CompanyBillingDTO updateCompanyBilling(Long id, CompanyBillingDTO dto);
    
    void deleteCompanyBilling(Long id);
    
    CompanyBillingDTO incrementCurrentPeriodUsage(Long id, Integer leadCount);
    
    CompanyBillingDTO resetCurrentPeriod(Long id);
    
    boolean hasReachedDeviceLimit(Long id);
    
    boolean hasReachedPromotionLimit(Long id);
}
