package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repository.FraudRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for FraudRuleServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class FraudRuleServiceImplTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @InjectMocks
    private FraudRuleServiceImpl fraudRuleService;

    private Company company;
    private FraudRule rule;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        rule = new FraudRule();
        rule.setId(1L);
        rule.setName("Velocity Rule");
        rule.setDescription("Test velocity rule");
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
    }

    // ==================== QUERY TESTS ====================

    @Test
    void testGetRulesByCompany() {
        when(fraudRuleRepository.findByCompany_Id(1L)).thenReturn(List.of(rule));

        List<FraudRule> result = fraudRuleService.getRulesByCompany(1L);

        assertEquals(1, result.size());
        assertEquals("Velocity Rule", result.get(0).getName());
        verify(fraudRuleRepository).findByCompany_Id(1L);
    }

    @Test
    void testGetActiveRulesByCompany() {
        when(fraudRuleRepository.findActiveRulesByCompany(1L)).thenReturn(List.of(rule));

        List<FraudRule> result = fraudRuleService.getActiveRulesByCompany(1L);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        verify(fraudRuleRepository).findActiveRulesByCompany(1L);
    }

    @Test
    void testGetRulesByType() {
        when(fraudRuleRepository.findByCompany_IdAndRuleType(1L, FraudRuleType.VELOCITY))
                .thenReturn(List.of(rule));

        List<FraudRule> result = fraudRuleService.getRulesByType(1L, FraudRuleType.VELOCITY);

        assertEquals(1, result.size());
        assertEquals(FraudRuleType.VELOCITY, result.get(0).getRuleType());
    }

    @Test
    void testGetRulesBySeverity() {
        when(fraudRuleRepository.findByCompany_IdAndSeverity(1L, FraudSeverity.HIGH))
                .thenReturn(List.of(rule));

        List<FraudRule> result = fraudRuleService.getRulesBySeverity(1L, FraudSeverity.HIGH);

        assertEquals(1, result.size());
        assertEquals(FraudSeverity.HIGH, result.get(0).getSeverity());
    }

    @Test
    void testGetAutoBlockRulesByCompany() {
        FraudRule autoBlockRule = new FraudRule();
        autoBlockRule.setId(2L);
        autoBlockRule.setName("Auto Block Rule");
        autoBlockRule.setAutoBlock(true);
        autoBlockRule.setIsActive(true);

        when(fraudRuleRepository.findAutoBlockRulesByCompany(1L))
                .thenReturn(List.of(autoBlockRule));

        List<FraudRule> result = fraudRuleService.getAutoBlockRulesByCompany(1L);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getAutoBlock());
    }

    @Test
    void testGetRuleById_Found() {
        when(fraudRuleRepository.findById(1L)).thenReturn(Optional.of(rule));

        Optional<FraudRule> result = fraudRuleService.getRuleById(1L);

        assertTrue(result.isPresent());
        assertEquals("Velocity Rule", result.get().getName());
    }

    @Test
    void testGetRuleById_NotFound() {
        when(fraudRuleRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<FraudRule> result = fraudRuleService.getRuleById(999L);

        assertFalse(result.isPresent());
    }

    // ==================== CRUD TESTS ====================

    @Test
    void testCreateRule_Success() {
        when(fraudRuleRepository.save(any(FraudRule.class))).thenReturn(rule);

        FraudRule created = fraudRuleService.createRule(rule);

        assertNotNull(created);
        assertEquals("Velocity Rule", created.getName());
        assertEquals(FraudRuleType.VELOCITY, created.getRuleType());
        verify(fraudRuleRepository).save(any(FraudRule.class));
    }

    @Test
    void testUpdateRule_AllFields() {
        FraudRule updateData = new FraudRule();
        updateData.setName("Updated Rule");
        updateData.setDescription("Updated description");
        updateData.setRuleType(FraudRuleType.DUPLICATE_DEVICE);
        updateData.setSeverity(FraudSeverity.CRITICAL);
        updateData.setIsActive(false);
        updateData.setAutoAlert(false);
        updateData.setAutoBlock(true);
        
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("maxDevices", 5);
        updateData.setConfiguration(newConfig);

        when(fraudRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(fraudRuleRepository.save(any(FraudRule.class))).thenReturn(rule);

        FraudRule updated = fraudRuleService.updateRule(1L, updateData);

        assertNotNull(updated);
        verify(fraudRuleRepository).save(any(FraudRule.class));
    }

    @Test
    void testUpdateRule_PartialFields() {
        FraudRule updateData = new FraudRule();
        updateData.setName("Updated Name Only");
        // Other fields are null

        when(fraudRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(fraudRuleRepository.save(any(FraudRule.class))).thenReturn(rule);

        FraudRule updated = fraudRuleService.updateRule(1L, updateData);

        assertNotNull(updated);
        verify(fraudRuleRepository).save(any(FraudRule.class));
    }

    @Test
    void testUpdateRule_NotFound() {
        FraudRule updateData = new FraudRule();
        updateData.setName("Updated Rule");

        when(fraudRuleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fraudRuleService.updateRule(999L, updateData));
    }

    @Test
    void testDeleteRule() {
        fraudRuleService.deleteRule(1L);
        verify(fraudRuleRepository).deleteById(1L);
    }

    @Test
    void testToggleRuleActive_FromActiveToInactive() {
        when(fraudRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(fraudRuleRepository.save(any(FraudRule.class))).thenReturn(rule);

        FraudRule toggled = fraudRuleService.toggleRuleActive(1L);

        assertNotNull(toggled);
        verify(fraudRuleRepository).save(any(FraudRule.class));
    }

    @Test
    void testToggleRuleActive_FromInactiveToActive() {
        rule.setIsActive(false);
        
        when(fraudRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(fraudRuleRepository.save(any(FraudRule.class))).thenReturn(rule);

        FraudRule toggled = fraudRuleService.toggleRuleActive(1L);

        assertNotNull(toggled);
        verify(fraudRuleRepository).save(any(FraudRule.class));
    }

    @Test
    void testToggleRuleActive_NotFound() {
        when(fraudRuleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fraudRuleService.toggleRuleActive(999L));
    }

    // ==================== UTILITY TESTS ====================

    @Test
    void testCountActiveRulesByCompany() {
        when(fraudRuleRepository.countActiveRulesByCompany(1L)).thenReturn(5L);

        long count = fraudRuleService.countActiveRulesByCompany(1L);

        assertEquals(5L, count);
        verify(fraudRuleRepository).countActiveRulesByCompany(1L);
    }

    @Test
    void testRuleNameExists_True() {
        when(fraudRuleRepository.existsByNameAndCompany_Id("Velocity Rule", 1L)).thenReturn(true);

        boolean exists = fraudRuleService.ruleNameExists("Velocity Rule", 1L);

        assertTrue(exists);
    }

    @Test
    void testRuleNameExists_False() {
        when(fraudRuleRepository.existsByNameAndCompany_Id("Non-existent Rule", 1L)).thenReturn(false);

        boolean exists = fraudRuleService.ruleNameExists("Non-existent Rule", 1L);

        assertFalse(exists);
    }

    // ==================== EDGE CASES ====================

    @Test
    void testGetRulesByCompany_EmptyList() {
        when(fraudRuleRepository.findByCompany_Id(999L)).thenReturn(List.of());

        List<FraudRule> result = fraudRuleService.getRulesByCompany(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRulesByType_EmptyList() {
        when(fraudRuleRepository.findByCompany_IdAndRuleType(1L, FraudRuleType.BLACKLIST))
                .thenReturn(List.of());

        List<FraudRule> result = fraudRuleService.getRulesByType(1L, FraudRuleType.BLACKLIST);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCountActiveRulesByCompany_Zero() {
        when(fraudRuleRepository.countActiveRulesByCompany(999L)).thenReturn(0L);

        long count = fraudRuleService.countActiveRulesByCompany(999L);

        assertEquals(0L, count);
    }
}
