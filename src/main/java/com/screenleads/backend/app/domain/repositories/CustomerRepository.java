package com.screenleads.backend.app.domain.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.UserSegment;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByEmail(String email);
    
    Optional<Customer> findByPhone(String phone);
    
    List<Customer> findBySegment(UserSegment segment);
    
    List<Customer> findByEmailVerified(Boolean emailVerified);
    
    @Query("SELECT c FROM Customer c WHERE c.email = :email OR c.phone = :phone")
    Optional<Customer> findByEmailOrPhone(@Param("email") String email, @Param("phone") String phone);
    
    @Query("SELECT c FROM Customer c WHERE c.email LIKE %:searchTerm% OR c.phone LIKE %:searchTerm% OR c.firstName LIKE %:searchTerm% OR c.lastName LIKE %:searchTerm%")
    List<Customer> searchCustomers(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT c FROM Customer c WHERE c.marketingOptIn = true AND c.emailVerified = true")
    List<Customer> findMarketingEligibleCustomers();
    
    @Query("SELECT c FROM Customer c WHERE c.lastInteractionAt < :since")
    List<Customer> findInactiveCustomers(@Param("since") Instant since);
    
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    Long countNewCustomersBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    // Query method for finding all customers belonging to a company
    // Note: Customer doesn't have a direct company relationship, so this needs to be implemented
    // based on your business logic (e.g., through redemptions or other relationships)
    @Query("SELECT DISTINCT c FROM Customer c WHERE c.id IN (SELECT pr.customer.id FROM PromotionRedemption pr WHERE pr.company.id = :companyId)")
    List<Customer> findByCompanyId(@Param("companyId") Long companyId);
}