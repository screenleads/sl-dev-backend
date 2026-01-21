package com.screenleads.backend.app.application.service.impl;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.PromotionRedemptionRepository;
import com.screenleads.backend.app.infrastructure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for FraudDetectionServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceImplTest {

    @Mock
    private FraudRuleRepository ruleRepository;

    @Mock
    private FraudAlertRepository alertRepository;

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private PromotionRedemptionRepository redemptionRepository;

    @InjectMocks
    private FraudDetectionServiceImpl fraudService;

    private Company company;
    private FraudRule rule;
    private FraudAlert alert;
    private Blacklist blacklistEntry;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        rule = new FraudRule();
        rule.setId(1L);
        rule.setName("Test Fraud Rule");
        rule.setDescription("Test Description");
        rule.setRuleType(FraudRuleType.VELOCITY);
        rule.setSeverity(FraudSeverity.HIGH);
        rule.setCompany(company);
        rule.setIsActive(true);
        rule.setAutoAlert(true);
        rule.setAutoBlock(false);
        
        Map<String, Object> config = new HashMap<>();
        config.put("maxTransactions", 10);
        config.put("timeWindow", 60);
        rule.setConfiguration(config);

        alert = new FraudAlert();
        alert.setId(1L);
        alert.setCompany(company);
        alert.setRule(rule);
        alert.setSeverity(FraudSeverity.HIGH);
        alert.setStatus(FraudAlertStatus.PENDING);
        alert.setRelatedEntityType("REDEMPTION");
        alert.setRelatedEntityId(100L);
        alert.setTitle("Suspicious activity");
        alert.setDescription("Suspicious activity detected");

        blacklistEntry = new Blacklist();
        blacklistEntry.setId(1L);
        blacklistEntry.setCompany(company);
        blacklistEntry.setBlacklistType(BlacklistType.EMAIL);
        blacklistEntry.setValue("fraud@example.com");
        blacklistEntry.setReason("Confirmed fraud");
        blacklistEntry.setIsActive(true);
    }

    // ==================== FRAUD RULE TESTS ====================

    @Test
    void testCreateRule_Success() {
        when(ruleRepository.save(any(FraudRule.class))).thenReturn(rule);

        FraudRule created = fraudService.createRule(rule);

        assertNotNull(created);
        assertEquals("Test Fraud Rule", created.getName());
        assertEquals(FraudRuleType.VELOCITY, created.getRuleType());
        verify(ruleRepository).save(any(FraudRule.class));
    }

    @Test
    void testUpdateRule_Success() {
        FraudRule updatedData = new FraudRule();
        updatedData.setName("Updated Rule");
        updatedData.setDescription("Updated Description");
        updatedData.setRuleType(FraudRuleType.DUPLICATE_DEVICE);
        updatedData.setSeverity(FraudSeverity.CRITICAL);
        updatedData.setIsActive(false);
        updatedData.setAutoAlert(false);
        updatedData.setAutoBlock(true);
        
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("maxDevices", 5);
        updatedData.setConfiguration(newConfig);

        when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(FraudRule.class))).thenReturn(rule);

        FraudRule result = fraudService.updateRule(1L, updatedData);

        assertNotNull(result);
        verify(ruleRepository).save(any(FraudRule.class));
    }

    @Test
    void testDeleteRule() {
        fraudService.deleteRule(1L);
        verify(ruleRepository).deleteById(1L);
    }

    @Test
    void testGetRule_Found() {
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));

        FraudRule result = fraudService.getRule(1L);

        assertNotNull(result);
        assertEquals("Test Fraud Rule", result.getName());
    }

    @Test
    void testGetRule_NotFound() {
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fraudService.getRule(999L));
    }

    @Test
    void testGetRulesByCompany() {
        when(ruleRepository.findByCompany_Id(1L)).thenReturn(List.of(rule));

        List<FraudRule> result = fraudService.getRulesByCompany(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getCompany().getId());
    }

    @Test
    void testGetActiveRulesByCompany() {
        when(ruleRepository.findByCompany_IdAndIsActiveTrue(1L)).thenReturn(List.of(rule));

        List<FraudRule> result = fraudService.getActiveRulesByCompany(1L);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    // ==================== FRAUD ALERT TESTS ====================

    @Test
    void testCreateAlert_Success() {
        when(alertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        FraudAlert created = fraudService.createAlert(alert);

        assertNotNull(created);
        assertEquals(FraudSeverity.HIGH, created.getSeverity());
        assertEquals(FraudAlertStatus.PENDING, created.getStatus());
        verify(alertRepository).save(any(FraudAlert.class));
    }

    @Test
    void testUpdateAlertStatus_ToPending() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        FraudAlert result = fraudService.updateAlertStatus(1L, FraudAlertStatus.PENDING, "Under review");

        assertNotNull(result);
        verify(alertRepository).save(any(FraudAlert.class));
    }

    @Test
    void testUpdateAlertStatus_ToConfirmed() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        FraudAlert result = fraudService.updateAlertStatus(1L, FraudAlertStatus.CONFIRMED, "Fraud confirmed");

        assertNotNull(result);
        verify(alertRepository).save(any(FraudAlert.class));
    }

    @Test
    void testUpdateAlertStatus_ToFalsePositive() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        FraudAlert result = fraudService.updateAlertStatus(1L, FraudAlertStatus.FALSE_POSITIVE, "False positive");

        assertNotNull(result);
        verify(alertRepository).save(any(FraudAlert.class));
    }

    @Test
    void testGetAlert_Found() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        FraudAlert result = fraudService.getAlert(1L);

        assertNotNull(result);
        assertEquals(FraudSeverity.HIGH, result.getSeverity());
    }

    @Test
    void testGetAlertsByCompany_WithPagination() {
        Page<FraudAlert> page = new PageImpl<>(List.of(alert));
        when(alertRepository.findByCompany_Id(eq(1L), any(PageRequest.class))).thenReturn(page);

        List<FraudAlert> result = fraudService.getAlertsByCompany(1L, 0, 10);

        assertEquals(1, result.size());
        verify(alertRepository).findByCompany_Id(eq(1L), any(PageRequest.class));
    }

    @Test
    void testGetPendingAlerts() {
        when(alertRepository.findAlertsByStatuses(eq(1L), anyList()))
                .thenReturn(List.of(alert));

        List<FraudAlert> result = fraudService.getPendingAlerts(1L);

        assertEquals(1, result.size());
        assertEquals(FraudAlertStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    void testGetAlertStatistics() {
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        
        when(alertRepository.countByCompany_IdAndStatus(1L, FraudAlertStatus.PENDING)).thenReturn(5L);
        when(alertRepository.countByCompany_IdAndStatus(1L, FraudAlertStatus.INVESTIGATING)).thenReturn(2L);
        when(alertRepository.countByCompany_IdAndStatus(1L, FraudAlertStatus.CONFIRMED)).thenReturn(3L);
        when(alertRepository.countByCompany_IdAndSeverity(1L, FraudSeverity.CRITICAL)).thenReturn(1L);
        when(alertRepository.countByCompany_IdAndSeverity(1L, FraudSeverity.HIGH)).thenReturn(4L);
        when(alertRepository.countRecentAlerts(eq(1L), any(LocalDateTime.class))).thenReturn(8L);

        Map<String, Object> stats = fraudService.getAlertStatistics(1L);

        assertNotNull(stats);
        assertEquals(1L, stats.get("companyId"));
        assertEquals(5L, stats.get("pendingAlerts"));
        assertEquals(2L, stats.get("investigatingAlerts"));
        assertEquals(3L, stats.get("confirmedFrauds"));
        assertEquals(1L, stats.get("criticalAlerts"));
        assertEquals(4L, stats.get("highAlerts"));
        assertEquals(8L, stats.get("alertsLast24h"));
    }

    // ==================== BLACKLIST TESTS ====================

    @Test
    void testAddToBlacklist_Success() {
        when(blacklistRepository.save(any(Blacklist.class))).thenReturn(blacklistEntry);

        Blacklist created = fraudService.addToBlacklist(blacklistEntry);

        assertNotNull(created);
        assertEquals(BlacklistType.EMAIL, created.getBlacklistType());
        assertEquals("fraud@example.com", created.getValue());
        verify(blacklistRepository).save(any(Blacklist.class));
    }

    @Test
    void testRemoveFromBlacklist() {
        fraudService.removeFromBlacklist(1L);
        verify(blacklistRepository).deleteById(1L);
    }

    @Test
    void testIsBlacklisted_True() {
        when(blacklistRepository.findEffectiveBlacklist(
                eq(1L), eq(BlacklistType.EMAIL), eq("fraud@example.com"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(blacklistEntry));

        boolean result = fraudService.isBlacklisted(1L, BlacklistType.EMAIL, "fraud@example.com");

        assertTrue(result);
    }

    @Test
    void testIsBlacklisted_False() {
        when(blacklistRepository.findEffectiveBlacklist(
                eq(1L), eq(BlacklistType.EMAIL), eq("safe@example.com"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        boolean result = fraudService.isBlacklisted(1L, BlacklistType.EMAIL, "safe@example.com");

        assertFalse(result);
    }

    @Test
    void testGetBlacklistByCompany() {
        when(blacklistRepository.findByCompany_IdAndIsActiveTrue(1L))
                .thenReturn(List.of(blacklistEntry));

        List<Blacklist> result = fraudService.getBlacklistByCompany(1L);

        assertEquals(1, result.size());
        assertEquals("fraud@example.com", result.get(0).getValue());
    }

    // ==================== FRAUD DETECTION TESTS ====================

    @Test
    void testCheckForFraud_WithVelocityRule() {
        Map<String, Object> context = new HashMap<>();
        context.put("deviceId", 123L);
        context.put("timestamp", LocalDateTime.now());

        when(ruleRepository.findByCompany_IdAndIsActiveTrue(1L)).thenReturn(List.of(rule));
        when(redemptionRepository.countByDevice_IdAndCreatedAtAfter(
                anyLong(), any(LocalDateTime.class))).thenReturn(5L);

        List<FraudAlert> alerts = fraudService.checkForFraud(1L, "REDEMPTION", 100L, context);

        assertNotNull(alerts);
        verify(ruleRepository).findByCompany_IdAndIsActiveTrue(1L);
    }

    @Test
    void testEvaluateVelocityRule_BelowThreshold() {
        Map<String, Object> context = new HashMap<>();
        context.put("deviceId", 123L);
        context.put("timestamp", LocalDateTime.now());

        when(redemptionRepository.countByDevice_IdAndCreatedAtAfter(
                anyLong(), any(LocalDateTime.class))).thenReturn(3L);

        boolean result = fraudService.evaluateVelocityRule(rule, context);

        assertFalse(result, "Should not trigger alert when below threshold");
    }

    @Test
    void testEvaluateVelocityRule_AboveThreshold() {
        Map<String, Object> context = new HashMap<>();
        context.put("deviceId", 123L);
        context.put("timestamp", LocalDateTime.now());

        when(redemptionRepository.countByDevice_IdAndCreatedAtAfter(
                anyLong(), any(LocalDateTime.class))).thenReturn(15L);

        boolean result = fraudService.evaluateVelocityRule(rule, context);

        assertTrue(result, "Should trigger alert when above threshold");
    }

    @Test
    void testEvaluateDuplicateDeviceRule_NoDuplicate() {
        FraudRule deviceRule = new FraudRule();
        deviceRule.setRuleType(FraudRuleType.DUPLICATE_DEVICE);
        Map<String, Object> config = new HashMap<>();
        config.put("maxRedemptionsPerDevice", 3);
        deviceRule.setConfiguration(config);

        Map<String, Object> context = new HashMap<>();
        context.put("deviceId", 123L);
        context.put("promotionId", 1L);

        when(redemptionRepository.countByDevice_IdAndPromotion_Id(123L, 1L)).thenReturn(2L);

        boolean result = fraudService.evaluateDuplicateDeviceRule(deviceRule, context);

        assertFalse(result, "Should not trigger when below max redemptions");
    }

    @Test
    void testEvaluateDuplicateDeviceRule_ExceedsLimit() {
        FraudRule deviceRule = new FraudRule();
        deviceRule.setRuleType(FraudRuleType.DUPLICATE_DEVICE);
        Map<String, Object> config = new HashMap<>();
        config.put("maxRedemptionsPerDevice", 3);
        deviceRule.setConfiguration(config);

        Map<String, Object> context = new HashMap<>();
        context.put("deviceId", 123L);
        context.put("promotionId", 1L);

        when(redemptionRepository.countByDevice_IdAndPromotion_Id(123L, 1L)).thenReturn(5L);

        boolean result = fraudService.evaluateDuplicateDeviceRule(deviceRule, context);

        assertTrue(result, "Should trigger when exceeding max redemptions");
    }

    // ==================== EDGE CASES ====================

    @Test
    void testCheckForFraud_NoActiveRules() {
        Map<String, Object> context = new HashMap<>();
        when(ruleRepository.findByCompany_IdAndIsActiveTrue(1L)).thenReturn(List.of());

        List<FraudAlert> alerts = fraudService.checkForFraud(1L, "REDEMPTION", 100L, context);

        assertNotNull(alerts);
        assertTrue(alerts.isEmpty(), "Should return empty list when no active rules");
    }

    @Test
    void testEvaluateVelocityRule_MissingContext() {
        Map<String, Object> emptyContext = new HashMap<>();

        boolean result = fraudService.evaluateVelocityRule(rule, emptyContext);

        assertFalse(result, "Should not trigger with missing context data");
    }

    @Test
    void testGetAlertsByCompany_EmptyResults() {
        Page<FraudAlert> emptyPage = new PageImpl<>(List.of());
        when(alertRepository.findByCompany_Id(eq(1L), any(PageRequest.class))).thenReturn(emptyPage);

        List<FraudAlert> result = fraudService.getAlertsByCompany(1L, 0, 10);

        assertTrue(result.isEmpty());
    }
}
