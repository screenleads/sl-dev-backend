package com.screenleads.backend.app.application.service.impl;

import com.screenleads.backend.app.application.service.FraudDetectionService;
import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.PromotionRedemptionRepository;
import com.screenleads.backend.app.domain.repository.FraudRuleRepository;
import com.screenleads.backend.app.domain.repository.FraudAlertRepository;
import com.screenleads.backend.app.domain.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final FraudRuleRepository ruleRepository;
    private final FraudAlertRepository alertRepository;
    private final BlacklistRepository blacklistRepository;
    private final PromotionRedemptionRepository redemptionRepository;

    @Override
    public FraudRule createRule(FraudRule rule) {
        log.info("Creating fraud rule: {} for company: {}", rule.getName(), rule.getCompany().getId());
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        return ruleRepository.save(rule);
    }

    @Override
    public FraudRule updateRule(Long ruleId, FraudRule updatedRule) {
        FraudRule existing = getRule(ruleId);

        existing.setName(updatedRule.getName());
        existing.setDescription(updatedRule.getDescription());
        existing.setRuleType(updatedRule.getRuleType());
        existing.setSeverity(updatedRule.getSeverity());
        existing.setConfiguration(updatedRule.getConfiguration());
        existing.setIsActive(updatedRule.getIsActive());
        existing.setAutoAlert(updatedRule.getAutoAlert());
        existing.setAutoBlock(updatedRule.getAutoBlock());
        existing.setUpdatedAt(LocalDateTime.now());

        return ruleRepository.save(existing);
    }

    @Override
    public void deleteRule(Long ruleId) {
        log.info("Deleting fraud rule: {}", ruleId);
        ruleRepository.deleteById(ruleId);
    }

    @Override
    @Transactional(readOnly = true)
    public FraudRule getRule(Long ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Fraud rule not found: " + ruleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudRule> getRulesByCompany(Long companyId) {
        return ruleRepository.findByCompany_Id(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudRule> getActiveRulesByCompany(Long companyId) {
        return ruleRepository.findActiveRulesByCompany(companyId);
    }

    @Override
    public FraudAlert createAlert(FraudAlert alert) {
        alert.setDetectedAt(LocalDateTime.now());
        alert.setCreatedAt(LocalDateTime.now());
        alert.setUpdatedAt(LocalDateTime.now());

        log.warn("Fraud alert created: {} - Severity: {}", alert.getTitle(), alert.getSeverity());

        FraudAlert saved = alertRepository.save(alert);

        // Si la regla tiene auto-block, agregar a blacklist
        FraudRule rule = alert.getRule();
        if (rule != null && rule.getAutoBlock()) {
            autoBlockFromAlert(saved);
        }

        return saved;
    }

    private void autoBlockFromAlert(FraudAlert alert) {
        Map<String, Object> evidence = alert.getEvidence();
        if (evidence == null) {
            return;
        }

        // Intentar bloquear IP si está en la evidencia
        if (evidence.containsKey("ipAddress")) {
            String ip = evidence.get("ipAddress").toString();
            addToBlacklistIfNotExists(alert.getCompany(), BlacklistType.IP_ADDRESS, ip,
                    "Auto-blocked by fraud rule: " + alert.getRule().getName(), alert);
        }

        // Intentar bloquear Device ID si está en la evidencia
        if (evidence.containsKey("deviceId")) {
            String deviceId = evidence.get("deviceId").toString();
            addToBlacklistIfNotExists(alert.getCompany(), BlacklistType.DEVICE_ID, deviceId,
                    "Auto-blocked by fraud rule: " + alert.getRule().getName(), alert);
        }
    }

    private void addToBlacklistIfNotExists(Company company, BlacklistType type, String value,
            String reason, FraudAlert alert) {
        Optional<Blacklist> existing = blacklistRepository
                .findEffectiveBlacklist(company.getId(), type, value, LocalDateTime.now());

        if (existing.isEmpty()) {
            Blacklist blacklist = Blacklist.builder()
                    .company(company)
                    .blacklistType(type)
                    .value(value)
                    .reason(reason)
                    .isActive(true)
                    .alert(alert)
                    .createdAt(LocalDateTime.now())
                    .build();

            blacklistRepository.save(blacklist);
            log.info("Auto-added to blacklist: {} - {}", type, value);
        }
    }

    @Override
    public FraudAlert updateAlertStatus(Long alertId, FraudAlertStatus status, String notes) {
        FraudAlert alert = getAlert(alertId);

        alert.setStatus(status);
        alert.setResolutionNotes(notes);
        alert.setUpdatedAt(LocalDateTime.now());

        if (status == FraudAlertStatus.RESOLVED || status == FraudAlertStatus.FALSE_POSITIVE) {
            alert.setResolvedAt(LocalDateTime.now());
        }

        return alertRepository.save(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public FraudAlert getAlert(Long alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Fraud alert not found: " + alertId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getAlertsByCompany(Long companyId, int page, int size) {
        List<FraudAlert> allAlerts = alertRepository.findByCompany_Id(companyId);
        int start = page * size;
        int end = Math.min(start + size, allAlerts.size());
        return start < allAlerts.size() ? allAlerts.subList(start, end) : List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getPendingAlerts(Long companyId) {
        return alertRepository.findAlertsByStatuses(
                companyId,
                Arrays.asList(FraudAlertStatus.PENDING, FraudAlertStatus.INVESTIGATING));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAlertStatistics(Long companyId) {
        long pending = alertRepository.countByCompany_IdAndStatus(companyId, FraudAlertStatus.PENDING);
        long investigating = alertRepository.countByCompany_IdAndStatus(companyId, FraudAlertStatus.INVESTIGATING);
        long confirmed = alertRepository.countByCompany_IdAndStatus(companyId, FraudAlertStatus.CONFIRMED);

        long critical = alertRepository.countByCompany_IdAndSeverity(companyId, FraudSeverity.CRITICAL);
        long high = alertRepository.countByCompany_IdAndSeverity(companyId, FraudSeverity.HIGH);

        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        long recent = alertRepository.countRecentAlerts(companyId, last24h);

        Map<String, Object> stats = new HashMap<>();
        stats.put("companyId", companyId);
        stats.put("pendingAlerts", pending);
        stats.put("investigatingAlerts", investigating);
        stats.put("confirmedFrauds", confirmed);
        stats.put("criticalAlerts", critical);
        stats.put("highAlerts", high);
        stats.put("alertsLast24h", recent);

        return stats;
    }

    @Override
    public Blacklist addToBlacklist(Blacklist entry) {
        entry.setCreatedAt(LocalDateTime.now());
        log.info("Adding to blacklist: {} - {}", entry.getBlacklistType(), entry.getValue());
        return blacklistRepository.save(entry);
    }

    @Override
    public void removeFromBlacklist(Long blacklistId) {
        log.info("Removing from blacklist: {}", blacklistId);
        blacklistRepository.deleteById(blacklistId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted(Long companyId, BlacklistType type, String value) {
        Optional<Blacklist> entry = blacklistRepository
                .findEffectiveBlacklist(companyId, type, value, LocalDateTime.now());
        return entry.isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blacklist> getBlacklistByCompany(Long companyId) {
        return blacklistRepository.findByCompany_IdAndIsActiveTrue(companyId);
    }

    @Override
    @Scheduled(cron = "0 0 3 * * ?") // Ejecuta a las 3 AM diariamente
    public void expireOldBlacklists() {
        List<Blacklist> expired = blacklistRepository.findExpiredBlacklists(LocalDateTime.now());

        for (Blacklist entry : expired) {
            entry.setIsActive(false);
            blacklistRepository.save(entry);
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} blacklist entries", expired.size());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> checkForFraud(Long companyId, String entityType, Long entityId,
            Map<String, Object> context) {
        List<FraudRule> rules = getActiveRulesByCompany(companyId);
        List<FraudAlert> alerts = new ArrayList<>();

        for (FraudRule rule : rules) {
            boolean fraudDetected = false;

            switch (rule.getRuleType()) {
                case VELOCITY:
                    fraudDetected = evaluateVelocityRule(rule, context);
                    break;
                case DUPLICATE_DEVICE:
                    fraudDetected = evaluateDuplicateDeviceRule(rule, context);
                    break;
                case LOCATION_ANOMALY:
                    fraudDetected = evaluateLocationAnomalyRule(rule, context);
                    break;
                case BLACKLIST:
                    fraudDetected = evaluateBlacklistRule(rule, context);
                    break;
                default:
                    break;
            }

            if (fraudDetected && rule.getAutoAlert()) {
                FraudAlert alert = createFraudAlert(rule, entityType, entityId, context);
                alerts.add(alert);
            }
        }

        return alerts;
    }

    private FraudAlert createFraudAlert(FraudRule rule, String entityType, Long entityId,
            Map<String, Object> context) {
        return FraudAlert.builder()
                .company(rule.getCompany())
                .rule(rule)
                .severity(rule.getSeverity())
                .status(FraudAlertStatus.PENDING)
                .title("Fraud detected: " + rule.getName())
                .description(rule.getDescription())
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .evidence(context)
                .detectedAt(LocalDateTime.now())
                .confidenceScore(calculateConfidence(rule, context))
                .build();
    }

    private int calculateConfidence(FraudRule rule, Map<String, Object> context) {
        // Implementación simplificada - podría ser más sofisticada
        return switch (rule.getSeverity()) {
            case CRITICAL -> 95;
            case HIGH -> 85;
            case MEDIUM -> 70;
            case LOW -> 50;
        };
    }

    @Override
    public boolean evaluateVelocityRule(FraudRule rule, Map<String, Object> context) {
        Map<String, Object> config = rule.getConfiguration();
        if (config == null) {
            return false;
        }

        Integer maxRedemptions = getIntValue(config, "maxRedemptions", 10);
        Integer timeWindowMinutes = getIntValue(config, "timeWindowMinutes", 60);

        Long deviceId = getLongValue(context, "deviceId");
        if (deviceId == null) {
            return false;
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(timeWindowMinutes);
        long recentCount = redemptionRepository.countByDevice_IdAndCreatedAtAfter(deviceId, since);

        return recentCount >= maxRedemptions;
    }

    @Override
    public boolean evaluateDuplicateDeviceRule(FraudRule rule, Map<String, Object> context) {
        Map<String, Object> config = rule.getConfiguration();
        if (config == null) {
            return false;
        }

        Integer maxRedemptionsPerDevice = getIntValue(config, "maxRedemptionsPerDevice", 3);

        Long deviceId = getLongValue(context, "deviceId");
        Long promotionId = getLongValue(context, "promotionId");

        if (deviceId == null || promotionId == null) {
            return false;
        }

        long count = redemptionRepository.countByDevice_IdAndPromotion_Id(deviceId, promotionId);

        return count >= maxRedemptionsPerDevice;
    }

    @Override
    public boolean evaluateLocationAnomalyRule(FraudRule rule, Map<String, Object> context) {
        Map<String, Object> config = rule.getConfiguration();
        if (config == null) {
            return false;
        }

        Integer maxDistanceKm = getIntValue(config, "maxDistanceKm", 100);
        Integer timeWindowMinutes = getIntValue(config, "timeWindowMinutes", 5);

        // Implementación simplificada - requeriría tracking de ubicaciones previas
        Double lastLatitude = getDoubleValue(context, "lastLatitude");
        Double lastLongitude = getDoubleValue(context, "lastLongitude");
        Double currentLatitude = getDoubleValue(context, "currentLatitude");
        Double currentLongitude = getDoubleValue(context, "currentLongitude");

        if (lastLatitude == null || lastLongitude == null ||
                currentLatitude == null || currentLongitude == null) {
            return false;
        }

        double distance = calculateDistance(lastLatitude, lastLongitude,
                currentLatitude, currentLongitude);

        return distance / 1000 > maxDistanceKm; // Convertir metros a km
    }

    private boolean evaluateBlacklistRule(FraudRule rule, Map<String, Object> context) {
        Long companyId = rule.getCompany().getId();

        // Revisar IP
        String ipAddress = getStringValue(context, "ipAddress");
        if (ipAddress != null && isBlacklisted(companyId, BlacklistType.IP_ADDRESS, ipAddress)) {
            return true;
        }

        // Revisar Device ID
        String deviceId = getStringValue(context, "deviceId");
        if (deviceId != null && isBlacklisted(companyId, BlacklistType.DEVICE_ID, deviceId)) {
            return true;
        }

        // Revisar Email
        String email = getStringValue(context, "email");
        if (email != null && isBlacklisted(companyId, BlacklistType.EMAIL, email)) {
            return true;
        }

        return false;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radio de la Tierra en metros

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    // Helper methods
    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
