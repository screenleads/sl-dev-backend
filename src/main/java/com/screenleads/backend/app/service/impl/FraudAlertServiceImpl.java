package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repository.FraudAlertRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.service.FraudAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service implementation for Fraud Detection Alerts
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FraudAlertServiceImpl implements FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getAlertsByCompany(Long companyId) {
        log.debug("Getting fraud alerts for company: {}", companyId);
        return fraudAlertRepository.findByCompany_Id(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getAlertsByRule(Long ruleId) {
        log.debug("Getting fraud alerts for rule: {}", ruleId);
        return fraudAlertRepository.findByRule_Id(ruleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getAlertsByStatus(Long companyId, FraudAlertStatus status) {
        log.debug("Getting fraud alerts for company: {} and status: {}", companyId, status);
        return fraudAlertRepository.findByCompany_IdAndStatus(companyId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getAlertsBySeverity(Long companyId, FraudSeverity severity) {
        log.debug("Getting fraud alerts for company: {} and severity: {}", companyId, severity);
        return fraudAlertRepository.findByCompany_IdAndSeverity(companyId, severity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getPendingAlertsByCompany(Long companyId) {
        log.debug("Getting pending fraud alerts for company: {}", companyId);
        return fraudAlertRepository.findPendingAlertsByCompany(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getRecentAlertsByCompany(Long companyId, int days) {
        log.debug("Getting recent fraud alerts for company: {} (last {} days)", companyId, days);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return fraudAlertRepository.findRecentAlertsByCompany(companyId, since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getAlertsByRelatedEntity(Long companyId, String entityType, Long entityId) {
        log.debug("Getting fraud alerts for company: {}, entityType: {}, entityId: {}", 
                  companyId, entityType, entityId);
        return fraudAlertRepository.findByCompany_IdAndRelatedEntityTypeAndRelatedEntityId(
            companyId, entityType, entityId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getHighPriorityAlertsByCompany(Long companyId) {
        log.debug("Getting high priority fraud alerts for company: {}", companyId);
        return fraudAlertRepository.findHighPriorityAlertsByCompany(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FraudAlert> getAlertById(Long id) {
        log.debug("Getting fraud alert by id: {}", id);
        return fraudAlertRepository.findById(id);
    }

    @Override
    public FraudAlert createAlert(FraudAlert alert) {
        log.info("Creating fraud alert for company: {}", alert.getCompany().getId());
        if (alert.getDetectedAt() == null) {
            alert.setDetectedAt(LocalDateTime.now());
        }
        return fraudAlertRepository.save(alert);
    }

    @Override
    public FraudAlert createAlertFromRule(
            FraudRule rule,
            Long companyId,
            String title,
            String description,
            String relatedEntityType,
            Long relatedEntityId,
            Map<String, Object> evidence,
            Integer confidenceScore) {
        
        log.info("Creating fraud alert from rule: {} for company: {}", rule.getName(), companyId);
        
        FraudAlert alert = FraudAlert.builder()
            .rule(rule)
            .company(rule.getCompany())
            .severity(rule.getSeverity())
            .status(FraudAlertStatus.PENDING)
            .title(title)
            .description(description)
            .relatedEntityType(relatedEntityType)
            .relatedEntityId(relatedEntityId)
            .evidence(evidence)
            .confidenceScore(confidenceScore)
            .detectedAt(LocalDateTime.now())
            .build();

        return fraudAlertRepository.save(alert);
    }

    @Override
    public FraudAlert updateAlertStatus(Long id, FraudAlertStatus newStatus) {
        log.info("Updating fraud alert status: {} to {}", id, newStatus);
        FraudAlert alert = fraudAlertRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Fraud alert not found with id: " + id));
        
        alert.setStatus(newStatus);
        return fraudAlertRepository.save(alert);
    }

    @Override
    public FraudAlert resolveAlert(Long id, Long userId, String resolutionNotes) {
        log.info("Resolving fraud alert: {} by user: {}", id, userId);
        FraudAlert alert = fraudAlertRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Fraud alert not found with id: " + id));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        alert.setStatus(FraudAlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(user);
        alert.setResolutionNotes(resolutionNotes);
        
        return fraudAlertRepository.save(alert);
    }

    @Override
    public FraudAlert updateAlert(Long id, FraudAlert alertDetails) {
        log.info("Updating fraud alert: {}", id);
        FraudAlert alert = fraudAlertRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Fraud alert not found with id: " + id));

        if (alertDetails.getTitle() != null) {
            alert.setTitle(alertDetails.getTitle());
        }
        if (alertDetails.getDescription() != null) {
            alert.setDescription(alertDetails.getDescription());
        }
        if (alertDetails.getStatus() != null) {
            alert.setStatus(alertDetails.getStatus());
        }
        if (alertDetails.getSeverity() != null) {
            alert.setSeverity(alertDetails.getSeverity());
        }
        if (alertDetails.getEvidence() != null) {
            alert.setEvidence(alertDetails.getEvidence());
        }
        if (alertDetails.getConfidenceScore() != null) {
            alert.setConfidenceScore(alertDetails.getConfidenceScore());
        }

        return fraudAlertRepository.save(alert);
    }

    @Override
    public void deleteAlert(Long id) {
        log.info("Deleting fraud alert: {}", id);
        fraudAlertRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAlertsByStatus(Long companyId, FraudAlertStatus status) {
        log.debug("Counting fraud alerts by status: {} for company: {}", status, companyId);
        return fraudAlertRepository.countByCompany_IdAndStatus(companyId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAlertsBySeverity(Long companyId, FraudSeverity severity) {
        log.debug("Counting fraud alerts by severity: {} for company: {}", severity, companyId);
        return fraudAlertRepository.countByCompany_IdAndSeverity(companyId, severity);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getAlertStatistics(Long companyId) {
        log.debug("Getting fraud alert statistics for company: {}", companyId);
        
        Map<String, Long> stats = new HashMap<>();
        
        // Count by status
        stats.put("pending", countAlertsByStatus(companyId, FraudAlertStatus.PENDING));
        stats.put("investigating", countAlertsByStatus(companyId, FraudAlertStatus.INVESTIGATING));
        stats.put("confirmed", countAlertsByStatus(companyId, FraudAlertStatus.CONFIRMED));
        stats.put("falsePositive", countAlertsByStatus(companyId, FraudAlertStatus.FALSE_POSITIVE));
        stats.put("resolved", countAlertsByStatus(companyId, FraudAlertStatus.RESOLVED));
        
        // Count by severity
        stats.put("low", countAlertsBySeverity(companyId, FraudSeverity.LOW));
        stats.put("medium", countAlertsBySeverity(companyId, FraudSeverity.MEDIUM));
        stats.put("high", countAlertsBySeverity(companyId, FraudSeverity.HIGH));
        stats.put("critical", countAlertsBySeverity(companyId, FraudSeverity.CRITICAL));
        
        return stats;
    }
}
