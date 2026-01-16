package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.domain.model.AuthMethod;
import com.screenleads.backend.app.web.dto.CustomerDTO;

public interface CustomerService {
    
    List<CustomerDTO> getAllCustomers();
    
    Optional<CustomerDTO> getCustomerById(Long id);
    
    Optional<CustomerDTO> getCustomerByEmail(String email);
    
    Optional<CustomerDTO> getCustomerByPhone(String phone);
    
    List<CustomerDTO> searchCustomers(String searchTerm);
    
    CustomerDTO createCustomer(CustomerDTO dto);
    
    CustomerDTO updateCustomer(Long id, CustomerDTO dto);
    
    void deleteCustomer(Long id);
    
    CustomerDTO addAuthMethod(Long id, AuthMethod authMethod);
    
    CustomerDTO incrementRedemptions(Long id);
    
    CustomerDTO verifyEmail(Long id);
    
    CustomerDTO verifyPhone(Long id);
    
    List<CustomerDTO> getMarketingEligibleCustomers();
}
