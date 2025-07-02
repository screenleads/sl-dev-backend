package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
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
        if (exist.isPresent())
            return convertToDTO(exist.get());
        Company savedCompany = companyRepository.save(company);
        return convertToDTO(savedCompany);
    }

    @Override
    public CompanyDTO updateCompany(Long id, CompanyDTO companyDTO) {
        Company company = companyRepository.findById(id).orElseThrow();
        company.setName(companyDTO.name());
        company.setLogo(companyDTO.logo());
        company.setObservations(companyDTO.observations());
        companyDTO.advices();
        // company.setAdvices(companyDTO.advices());
        // company.setDevices(companyDTO.devices());
        Company updatedCompany = companyRepository.save(company);
        return convertToDTO(updatedCompany);
    }

    @Override
    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }

    // Convert Company Entity to CompanyDTO
    private CompanyDTO convertToDTO(Company company) {
        return new CompanyDTO(company.getId(), company.getName(), company.getObservations(), company.getLogo(),
                company.getDevices(), company.getAdvices());
    }

    // Convert CompanyDTO to Company Entity
    private Company convertToEntity(CompanyDTO companyDTO) {
        Company company = new Company();
        company.setId(companyDTO.id());
        company.setName(companyDTO.name());
        company.setLogo(companyDTO.logo());
        company.setLogo(mediaRepository.findById(companyDTO.logo().getId()).get());
        company.setObservations(companyDTO.observations());
        List<Advice> advices = companyDTO.advices().stream().map(adviceDTO -> {
            Optional<Advice> advice = adviceRepository.findById(adviceDTO.getId());
            if (advice.isPresent()) {
                Advice aux = advice.get();
                aux.setCompany(company);
                System.out.println("Llega aqui 1");

                return advice.get();
            } else {
                System.out.println("Llega aqui 2");

                return null;
            }
        }).collect(Collectors.toList());

        company.setAdvices(advices);

        List<Device> devices = companyDTO.advices().stream().map(deviceDTO -> {
            Optional<Device> device = deviceRepository.findById(deviceDTO.getId());
            if (device.isPresent()) {
                Device aux = device.get();
                aux.setCompany(company);
                System.out.println("Llega aqui 1");

                return device.get();
            } else {
                System.out.println("Llega aqui 2");

                return null;
            }
        }).collect(Collectors.toList());

        company.setDevices(devices);
        return company;
    }
}
