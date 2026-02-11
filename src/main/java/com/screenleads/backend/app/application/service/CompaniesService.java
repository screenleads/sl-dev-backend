package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.CompanyDTO;

public interface CompaniesService {
    List<CompanyDTO> getAllCompanies();

    Optional<CompanyDTO> getCompanyById(Long id);

    CompanyDTO saveCompany(CompanyDTO dto);

    CompanyDTO updateCompany(Long id, CompanyDTO dto);

    void deleteCompany(Long id);

    CompanyDTO syncStripeData(Long companyId) throws BillingException;

    String createCheckoutSession(Long companyId) throws BillingException;

    String createBillingPortalSession(Long companyId) throws BillingException;
}
