package com.screenleads.backend.app.domain.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCompanyIdAndIdentifierTypeAndIdentifier(
        Long companyId, LeadIdentifierType identifierType, String identifier
    );

    List<Customer> findByCompanyId(Long companyId);

    List<Customer> findByCompanyIdAndIdentifierContainingIgnoreCase(Long companyId, String identifierPart);
}