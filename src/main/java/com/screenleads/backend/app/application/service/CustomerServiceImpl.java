package com.screenleads.backend.app.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.application.service.util.IdentifierNormalizer;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final String CUSTOMER_NOT_FOUND = "Customer not found: ";
    private static final String COMPANY_NOT_FOUND = "Company not found: ";
    private static final String CUSTOMER_ALREADY_EXISTS = "Customer already exists for this identifier";
    private static final String IDENTIFIER_IN_USE = "Another customer already uses this identifier";

    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;

    @Override
    public Customer create(Long companyId,
                           LeadIdentifierType identifierType,
                           String identifier,
                           String firstName,
                           String lastName) {

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException(COMPANY_NOT_FOUND + companyId));

        String normalized = IdentifierNormalizer.normalize(identifierType, identifier);

        // Enforce unicidad (company, type, identifier)
        customerRepository.findByCompanyIdAndIdentifierTypeAndIdentifier(companyId, identifierType, normalized)
            .ifPresent(c -> { throw new IllegalStateException(CUSTOMER_ALREADY_EXISTS); });

        Customer c = Customer.builder()
                .company(company)
                .identifierType(identifierType)
                .identifier(normalized)
                .firstName(firstName)
                .lastName(lastName)
                .build();

        return customerRepository.save(c);
    }

    @Override
    public Customer update(Long id,
                           LeadIdentifierType identifierType,
                           String identifier,
                           String firstName,
                           String lastName) {

        Customer existing = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(CUSTOMER_NOT_FOUND + id));

        String normalized = IdentifierNormalizer.normalize(identifierType, identifier);

        // Si cambia la clave única, comprobar colisión
        boolean keyChanged = existing.getIdentifierType() != identifierType
                || !existing.getIdentifier().equals(normalized);

        if (keyChanged) {
            customerRepository.findByCompanyIdAndIdentifierTypeAndIdentifier(
                    existing.getCompany().getId(), identifierType, normalized)
                .ifPresent(other -> {
                    if (!other.getId().equals(existing.getId())) {
                        throw new IllegalStateException(IDENTIFIER_IN_USE);
                    }
                });
        }

        existing.setIdentifierType(identifierType);
        existing.setIdentifier(normalized);
        existing.setFirstName(firstName);
        existing.setLastName(lastName);

        return customerRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(CUSTOMER_NOT_FOUND + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> list(Long companyId, String search) {
        if (companyId == null) {
            // Por simplicidad devolvemos todo; si prefieres obligar companyId, lanza excepción
            return customerRepository.findAll();
        }
        if (search == null || search.isBlank()) {
            return customerRepository.findByCompanyId(companyId);
        }
        return customerRepository.findByCompanyIdAndIdentifierContainingIgnoreCase(companyId, search.trim());
    }

    @Override
    public void delete(Long id) {
        Customer existing = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(CUSTOMER_NOT_FOUND + id));

        // Si quieres proteger borrado cuando tiene leads, compruébalo aquí:
        // if (existing.getLeads() != null && !existing.getLeads().isEmpty()) { ... }

        customerRepository.delete(existing);
    }
}
