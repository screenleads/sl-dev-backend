package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.FraudRule;
import com.screenleads.backend.app.domain.model.FraudRuleType;
import com.screenleads.backend.app.domain.model.FraudSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FraudRule entity
 */
@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {

    /**
     * Find all fraud rules by company ID
     */
    List<FraudRule> findByCompany_Id(Long companyId);

    /**
     * Find active fraud rules by company ID
     */
    @Query("SELECT r FROM FraudRule r WHERE r.company.id = :companyId AND r.isActive = true")
    List<FraudRule> findActiveRulesByCompany(@Param("companyId") Long companyId);

    /**
     * Find fraud rules by type and company
     */
    List<FraudRule> findByCompany_IdAndRuleType(Long companyId, FraudRuleType ruleType);

    /**
     * Find fraud rules by severity
     */
    List<FraudRule> findByCompany_IdAndSeverity(Long companyId, FraudSeverity severity);

    /**
     * Find fraud rules with auto-block enabled
     */
    @Query("SELECT r FROM FraudRule r WHERE r.company.id = :companyId AND r.autoBlock = true AND r.isActive = true")
    List<FraudRule> findAutoBlockRulesByCompany(@Param("companyId") Long companyId);

    /**
     * Find fraud rule by name and company
     */
    Optional<FraudRule> findByNameAndCompany_Id(String name, Long companyId);

    /**
     * Check if a fraud rule with the given name exists for a company
     */
    boolean existsByNameAndCompany_Id(String name, Long companyId);

    /**
     * Count active fraud rules by company
     */
    @Query("SELECT COUNT(r) FROM FraudRule r WHERE r.company.id = :companyId AND r.isActive = true")
    long countActiveRulesByCompany(@Param("companyId") Long companyId);
}
