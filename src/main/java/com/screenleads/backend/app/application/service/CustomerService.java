package com.screenleads.backend.app.application.service;

import java.util.List;

import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.LeadIdentifierType;

public interface CustomerService {

    Customer create(Long companyId,
                    LeadIdentifierType identifierType,
                    String identifier,
                    String firstName,
                    String lastName);

    Customer update(Long id,
                    LeadIdentifierType identifierType,
                    String identifier,
                    String firstName,
                    String lastName);

    Customer get(Long id);

    List<Customer> list(Long companyId, String search);

    void delete(Long id);
}
