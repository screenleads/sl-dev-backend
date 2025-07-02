package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.CompanyDTO;

public interface CompaniesService {
    List<CompanyDTO> getAllCompanies();

    Optional<CompanyDTO> getCompanyById(Long id);

    CompanyDTO saveCompany(CompanyDTO CompanyDTO);

    CompanyDTO updateCompany(Long id, CompanyDTO CompanyDTO);

    void deleteCompany(Long id);
}
