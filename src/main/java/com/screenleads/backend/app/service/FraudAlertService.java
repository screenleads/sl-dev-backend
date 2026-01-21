package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.FraudAlert;
import com.screenleads.backend.app.domain.model.FraudAlertStatus;
import com.screenleads.backend.app.domain.model.FraudRule;
import com.screenleads.backend.app.domain.model.FraudSeverity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for Fraud Detection Alerts
 */
public interface FraudAlertService {

    /**
     * Get all fraud alerts for a company
     */
    List<FraudAlert> getAlertsByCompany(Long companyId);

    /**
     * Get fraud alerts by rule
     */
    List<FraudAlert> getAlertsByRule(Long ruleId);

    /**
     * Get fraud alerts by status
     */
    List<FraudAlert> getAlertsByStatus(Long companyId, FraudAlertStatus status);

    /**
     * Get fraud alerts by severity
     */
    List<FraudAlert> getAlertsBySeverity(Long companyId, FraudSeverity severity);

    /**
     * Get pending alerts (PENDING or INVESTIGATING)
     */
    List<FraudAlert> getPendingAlertsByCompany(Long companyId);

    /**
     * Get recent alerts (last N days)
     */
    List<FraudAlert> getRecentAlertsByCompany(Long companyId, int days);

    /**
     * Get alerts by related entity
     */
    List<FraudAlert> getAlertsByRelatedEntity(Long companyId, String entityType, Long entityId);

    /**
     * Get high priority alerts
     */
    List<FraudAlert> getHighPriorityAlertsByCompany(Long companyId);

    /**
     * Get fraud alert by ID
     */
    Optional<FraudAlert> getAlertById(Long id);

    /**
     * Create a fraud alert manually
     */
    FraudAlert createAlert(FraudAlert alert);

    /**
     * Create a fraud alert from a rule trigger
     */
    FraudAlert createAlertFromRule(
        FraudRule rule,
        Long companyId,
        String title,
        String description,
        String relatedEntityType,
        Long relatedEntityId,
        Map<String, Object> evidence,
        Integer confidenceScore
    );

    /**
     * Update fraud alert status
     */
    FraudAlert updateAlertStatus(Long id, FraudAlertStatus newStatus);

    /**
     * Resolve a fraud alert
     */
    FraudAlert resolveAlert(Long id, Long userId, String resolutionNotes);

    /**
     * Update a fraud alert
     */
    FraudAlert updateAlert(Long id, FraudAlert alertDetails);

    /**
     * Delete a fraud alert
     */
    void deleteAlert(Long id);

    /**
     * Count alerts by status
     */
    long countAlertsByStatus(Long companyId, FraudAlertStatus status);

    /**
     * Count alerts by severity
     */
    long countAlertsBySeverity(Long companyId, FraudSeverity severity);

    /**
     * Get alert statistics for a company
     */
    Map<String, Long> getAlertStatistics(Long companyId);
}
