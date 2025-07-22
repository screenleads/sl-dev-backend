package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.web.dto.CompanyDTO;

@Service
public class CompaniesServiceImpl implements CompaniesService {

    private final CompanyRepository companyRepository;
    private final MediaRepository mediaRepository;
    private final AdviceRepository adviceRepository;
    private final DeviceRepository deviceRepository;

    public CompaniesServiceImpl(CompanyRepository companyRepository, MediaRepository mediaRepository,
                                 AdviceRepository adviceRepository, DeviceRepository deviceRepository) {
        this.companyRepository = companyRepository;
        this.mediaRepository = mediaRepository;
        this.adviceRepository = adviceRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    public List<CompanyDTO> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(CompanyDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CompanyDTO> getCompanyById(Long id) {
        return companyRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public CompanyDTO saveCompany(CompanyDTO companyDTO) {
        Company company = convertToEntity(companyDTO);
        Optional<Company> exist = companyRepository.findByName(company.getName());
        if (exist.isPresent()) {
            return convertToDTO(exist.get());
        }
        Company savedCompany = companyRepository.save(company);
        return convertToDTO(savedCompany);
    }

    @Override
    public CompanyDTO updateCompany(Long id, CompanyDTO companyDTO) {
        Company company = companyRepository.findById(id).orElseThrow();
        company.setName(companyDTO.name());
        company.setObservations(companyDTO.observations());

        if (companyDTO.logo() != null && companyDTO.logo().getId() != null) {
            company.setLogo(mediaRepository.findById(companyDTO.logo().getId()).orElse(null));
        } else {
            company.setLogo(null);
        }

        Company updatedCompany = companyRepository.save(company);
        return convertToDTO(updatedCompany);
    }

    @Override
    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }

    private CompanyDTO convertToDTO(Company company) {
        return new CompanyDTO(
                company.getId(),
                company.getName(),
                company.getObservations(),
                company.getLogo(),
                company.getDevices(),
                company.getAdvices()
        );
    }

    private Company convertToEntity(CompanyDTO companyDTO) {
        Company company = new Company();
        company.setId(companyDTO.id());
        company.setName(companyDTO.name());
        company.setObservations(companyDTO.observations());

        if (companyDTO.logo() != null && companyDTO.logo().getId() != null) {
            company.setLogo(mediaRepository.findById(companyDTO.logo().getId()).orElse(null));
        } else {
            company.setLogo(null);
        }

        List<Advice> advices = companyDTO.advices().stream()
                .map(dto -> adviceRepository.findById(dto.getId()).map(advice -> {
                    advice.setCompany(company);
                    return advice;
                }).orElse(null))
                .collect(Collectors.toList());
        company.setAdvices(advices);

        List<Device> devices = companyDTO.devices().stream()
                .map(dto -> deviceRepository.findById(dto.getId()).map(device -> {
                    device.setCompany(company);
                    return device;
                }).orElse(null))
                .collect(Collectors.toList());
        company.setDevices(devices);

        return company;
    }
}
