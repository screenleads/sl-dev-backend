package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.FraudRule;
import com.screenleads.backend.app.domain.model.FraudRuleType;
import com.screenleads.backend.app.domain.model.FraudSeverity;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Fraud Detection Rules
 */
public interface FraudRuleService {

    /**
     * Get all fraud rules for a company
     */
    List<FraudRule> getRulesByCompany(Long companyId);

    /**
     * Get active fraud rules for a company
     */
    List<FraudRule> getActiveRulesByCompany(Long companyId);

    /**
     * Get fraud rules by type
     */
    List<FraudRule> getRulesByType(Long companyId, FraudRuleType ruleType);

    /**
     * Get fraud rules by severity
     */
    List<FraudRule> getRulesBySeverity(Long companyId, FraudSeverity severity);

    /**
     * Get auto-block rules for a company
     */
    List<FraudRule> getAutoBlockRulesByCompany(Long companyId);

    /**
     * Get fraud rule by ID
     */
    Optional<FraudRule> getRuleById(Long id);

    /**
     * Create a new fraud rule
     */
    FraudRule createRule(FraudRule rule);

    /**
     * Update an existing fraud rule
     */
    FraudRule updateRule(Long id, FraudRule ruleDetails);

    /**
     * Delete a fraud rule
     */
    void deleteRule(Long id);

    /**
     * Toggle fraud rule active status
     */
    FraudRule toggleRuleActive(Long id);

    /**
     * Count active rules by company
     */
    long countActiveRulesByCompany(Long companyId);

    /**
     * Check if a rule name exists for a company
     */
    boolean ruleNameExists(String name, Long companyId);
}
