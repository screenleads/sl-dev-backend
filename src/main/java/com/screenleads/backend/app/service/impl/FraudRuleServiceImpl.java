package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.FraudRule;
import com.screenleads.backend.app.domain.model.FraudRuleType;
import com.screenleads.backend.app.domain.model.FraudSeverity;
import com.screenleads.backend.app.domain.repository.FraudRuleRepository;
import com.screenleads.backend.app.service.FraudRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for Fraud Detection Rules
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FraudRuleServiceImpl implements FraudRuleService {

    private final FraudRuleRepository fraudRuleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FraudRule> getRulesByCompany(Long companyId) {
        log.debug("Getting fraud rules for company: {}", companyId);
        return fraudRuleRepository.findByCompany_Id(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudRule> getActiveRulesByCompany(Long companyId) {
        log.debug("Getting active fraud rules for company: {}", companyId);
        return fraudRuleRepository.findActiveRulesByCompany(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudRule> getRulesByType(Long companyId, FraudRuleType ruleType) {
        log.debug("Getting fraud rules for company: {} and type: {}", companyId, ruleType);
        return fraudRuleRepository.findByCompany_IdAndRuleType(companyId, ruleType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudRule> getRulesBySeverity(Long companyId, FraudSeverity severity) {
        log.debug("Getting fraud rules for company: {} and severity: {}", companyId, severity);
        return fraudRuleRepository.findByCompany_IdAndSeverity(companyId, severity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudRule> getAutoBlockRulesByCompany(Long companyId) {
        log.debug("Getting auto-block fraud rules for company: {}", companyId);
        return fraudRuleRepository.findAutoBlockRulesByCompany(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FraudRule> getRuleById(Long id) {
        log.debug("Getting fraud rule by id: {}", id);
        return fraudRuleRepository.findById(id);
    }

    @Override
    public FraudRule createRule(FraudRule rule) {
        log.info("Creating fraud rule: {} for company: {}", rule.getName(), rule.getCompany().getId());
        return fraudRuleRepository.save(rule);
    }

    @Override
    public FraudRule updateRule(Long id, FraudRule ruleDetails) {
        log.info("Updating fraud rule: {}", id);
        FraudRule rule = fraudRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Fraud rule not found with id: " + id));

        if (ruleDetails.getName() != null) {
            rule.setName(ruleDetails.getName());
        }
        if (ruleDetails.getDescription() != null) {
            rule.setDescription(ruleDetails.getDescription());
        }
        if (ruleDetails.getRuleType() != null) {
            rule.setRuleType(ruleDetails.getRuleType());
        }
        if (ruleDetails.getSeverity() != null) {
            rule.setSeverity(ruleDetails.getSeverity());
        }
        if (ruleDetails.getConfiguration() != null) {
            rule.setConfiguration(ruleDetails.getConfiguration());
        }
        if (ruleDetails.getIsActive() != null) {
            rule.setIsActive(ruleDetails.getIsActive());
        }
        if (ruleDetails.getAutoAlert() != null) {
            rule.setAutoAlert(ruleDetails.getAutoAlert());
        }
        if (ruleDetails.getAutoBlock() != null) {
            rule.setAutoBlock(ruleDetails.getAutoBlock());
        }

        return fraudRuleRepository.save(rule);
    }

    @Override
    public void deleteRule(Long id) {
        log.info("Deleting fraud rule: {}", id);
        fraudRuleRepository.deleteById(id);
    }

    @Override
    public FraudRule toggleRuleActive(Long id) {
        log.info("Toggling fraud rule active status: {}", id);
        FraudRule rule = fraudRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Fraud rule not found with id: " + id));
        
        rule.setIsActive(!rule.getIsActive());
        return fraudRuleRepository.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveRulesByCompany(Long companyId) {
        log.debug("Counting active fraud rules for company: {}", companyId);
        return fraudRuleRepository.countActiveRulesByCompany(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean ruleNameExists(String name, Long companyId) {
        log.debug("Checking if fraud rule name exists: {} for company: {}", name, companyId);
        return fraudRuleRepository.existsByNameAndCompany_Id(name, companyId);
    }
}
