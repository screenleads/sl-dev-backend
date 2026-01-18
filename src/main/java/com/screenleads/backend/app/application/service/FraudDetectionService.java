package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.*;

import java.util.List;
import java.util.Map;

public interface FraudDetectionService {

    // Rule management
    FraudRule createRule(FraudRule rule);
    
    FraudRule updateRule(Long ruleId, FraudRule updatedRule);
    
    void deleteRule(Long ruleId);
    
    FraudRule getRule(Long ruleId);
    
    List<FraudRule> getRulesByCompany(Long companyId);
    
    List<FraudRule> getActiveRulesByCompany(Long companyId);

    // Alert management
    FraudAlert createAlert(FraudAlert alert);
    
    FraudAlert updateAlertStatus(Long alertId, FraudAlertStatus status, String notes);
    
    FraudAlert getAlert(Long alertId);
    
    List<FraudAlert> getAlertsByCompany(Long companyId, int page, int size);
    
    List<FraudAlert> getPendingAlerts(Long companyId);
    
    Map<String, Object> getAlertStatistics(Long companyId);

    // Blacklist management
    Blacklist addToBlacklist(Blacklist entry);
    
    void removeFromBlacklist(Long blacklistId);
    
    boolean isBlacklisted(Long companyId, BlacklistType type, String value);
    
    List<Blacklist> getBlacklistByCompany(Long companyId);
    
    void expireOldBlacklists();

    // Fraud detection
    List<FraudAlert> checkForFraud(Long companyId, String entityType, Long entityId, Map<String, Object> context);
    
    boolean evaluateVelocityRule(FraudRule rule, Map<String, Object> context);
    
    boolean evaluateDuplicateDeviceRule(FraudRule rule, Map<String, Object> context);
    
    boolean evaluateLocationAnomalyRule(FraudRule rule, Map<String, Object> context);
}
