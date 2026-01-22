package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repository.GeofenceRuleRepository;
import com.screenleads.backend.app.service.GeofenceRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeofenceRuleServiceImplTest {

    @Mock
    private GeofenceRuleRepository geofenceRuleRepository;

    @InjectMocks
    private GeofenceRuleServiceImpl geofenceRuleService;

    private Company company;
    private Promotion promotion;
    private GeofenceZone zone;
    private GeofenceRule rule;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .build();

        promotion = Promotion.builder()
                .id(100L)
                .name("Test Promotion")
                .company(company)
                .build();

        zone = GeofenceZone.builder()
                .id(200L)
                .name("Test Zone")
                .type(GeofenceType.CIRCLE)
                .geometry(Map.of("center_lat", 40.7128, "center_lng", -74.0060, "radius", 1000.0))
                .isActive(true)
                .build();

        rule = GeofenceRule.builder()
                .id(1L)
                .promotion(promotion)
                .zone(zone)
                .ruleType(RuleType.SHOW_INSIDE)
                .priority(10)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createRule_ShouldCreateNewRule() {
        // Given
        GeofenceRule newRule = GeofenceRule.builder()
                .promotion(promotion)
                .zone(zone)
                .ruleType(RuleType.SHOW_INSIDE)
                .priority(10)
                .isActive(true)
                .build();

        when(geofenceRuleRepository.save(any(GeofenceRule.class))).thenReturn(rule);

        // When
        GeofenceRule result = geofenceRuleService.createRule(newRule);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPromotion().getId()).isEqualTo(100L);
        assertThat(result.getZone().getId()).isEqualTo(200L);
        assertThat(result.getRuleType()).isEqualTo(RuleType.SHOW_INSIDE);
        assertThat(result.getPriority()).isEqualTo(10);
        assertThat(result.getIsActive()).isTrue();

        verify(geofenceRuleRepository, times(1)).save(any(GeofenceRule.class));
    }

    @Test
    void updateRule_ShouldUpdateExistingRule() {
        // Given
        GeofenceRule updatedData = GeofenceRule.builder()
                .ruleType(RuleType.HIDE_OUTSIDE)
                .priority(20)
                .isActive(false)
                .promotion(promotion)
                .zone(zone)
                .build();

        when(geofenceRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(geofenceRuleRepository.save(any(GeofenceRule.class))).thenReturn(rule);

        // When
        GeofenceRule result = geofenceRuleService.updateRule(1L, updatedData);

        // Then
        assertThat(result).isNotNull();
        verify(geofenceRuleRepository, times(1)).findById(1L);
        verify(geofenceRuleRepository, times(1)).save(rule);
        assertThat(rule.getRuleType()).isEqualTo(RuleType.HIDE_OUTSIDE);
        assertThat(rule.getPriority()).isEqualTo(20);
        assertThat(rule.getIsActive()).isFalse();
    }

    @Test
    void updateRule_WhenNotFound_ShouldThrowException() {
        // Given
        when(geofenceRuleRepository.findById(999L)).thenReturn(Optional.empty());

        GeofenceRule updatedData = GeofenceRule.builder().build();

        // When/Then
        assertThatThrownBy(() -> geofenceRuleService.updateRule(999L, updatedData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Geofence rule not found: 999");
    }

    @Test
    void deleteRule_ShouldDeleteExistingRule() {
        // Given
        doNothing().when(geofenceRuleRepository).deleteById(1L);

        // When
        geofenceRuleService.deleteRule(1L);

        // Then
        verify(geofenceRuleRepository, times(1)).deleteById(1L);
    }

    @Test
    void getRuleById_WhenFound_ShouldReturnRule() {
        // Given
        when(geofenceRuleRepository.findById(1L)).thenReturn(Optional.of(rule));

        // When
        Optional<GeofenceRule> result = geofenceRuleService.getRuleById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getPriority()).isEqualTo(10);

        verify(geofenceRuleRepository, times(1)).findById(1L);
    }

    @Test
    void getRuleById_WhenNotFound_ShouldReturnEmpty() {
        // Given
        when(geofenceRuleRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<GeofenceRule> result = geofenceRuleService.getRuleById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getRulesByCompany_ShouldReturnAllRulesOrderedByPriority() {
        // Given
        GeofenceRule rule2 = GeofenceRule.builder()
                .id(2L)
                .promotion(promotion)
                .zone(zone)
                .ruleType(RuleType.HIDE_OUTSIDE)
                .priority(5)
                .isActive(false)
                .build();

        when(geofenceRuleRepository.findByCompany_IdOrderByPriorityDescCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(rule, rule2));

        // When
        List<GeofenceRule> result = geofenceRuleService.getRulesByCompany(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPriority()).isEqualTo(10); // Higher priority first
        assertThat(result.get(1).getPriority()).isEqualTo(5);
    }

    @Test
    void getActiveRulesByCompany_ShouldReturnOnlyActiveRules() {
        // Given
        GeofenceRule activeRule = GeofenceRule.builder()
                .id(2L)
                .promotion(promotion)
                .zone(zone)
                .ruleType(RuleType.HIDE_OUTSIDE)
                .priority(15)
                .isActive(true)
                .build();

        when(geofenceRuleRepository.findByCompany_IdAndIsActiveTrueOrderByPriorityDescCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(activeRule, rule));

        // When
        List<GeofenceRule> result = geofenceRuleService.getActiveRulesByCompany(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(GeofenceRule::getIsActive);
    }

    @Test
    void getRulesByZone_ShouldReturnAllRulesForZone() {
        // Given
        GeofenceRule rule2 = GeofenceRule.builder()
                .id(2L)
                .promotion(promotion)
                .zone(zone)
                .priority(8)
                .build();

        when(geofenceRuleRepository.findByZone_IdOrderByPriorityDesc(200L))
                .thenReturn(Arrays.asList(rule, rule2));

        // When
        List<GeofenceRule> result = geofenceRuleService.getRulesByZone(200L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getZone().getId()).isEqualTo(200L);
    }

    @Test
    void getActiveRulesByZone_ShouldReturnOnlyActiveRulesForZone() {
        // Given
        when(geofenceRuleRepository.findByZone_IdAndIsActiveTrueOrderByPriorityDesc(200L))
                .thenReturn(Collections.singletonList(rule));

        // When
        List<GeofenceRule> result = geofenceRuleService.getActiveRulesByZone(200L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
        assertThat(result.get(0).getZone().getId()).isEqualTo(200L);
    }

    @Test
    void toggleRuleActive_WhenActivatingInactive_ShouldActivate() {
        // Given
        rule.setIsActive(false);
        when(geofenceRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(geofenceRuleRepository.save(any(GeofenceRule.class))).thenReturn(rule);

        // When
        GeofenceRule result = geofenceRuleService.toggleRuleActive(1L, true);

        // Then
        assertThat(result.getIsActive()).isTrue();
        verify(geofenceRuleRepository, times(1)).save(rule);
    }

    @Test
    void toggleRuleActive_WhenDeactivatingActive_ShouldDeactivate() {
        // Given
        rule.setIsActive(true);
        when(geofenceRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(geofenceRuleRepository.save(any(GeofenceRule.class))).thenReturn(rule);

        // When
        GeofenceRule result = geofenceRuleService.toggleRuleActive(1L, false);

        // Then
        assertThat(result.getIsActive()).isFalse();
        verify(geofenceRuleRepository, times(1)).save(rule);
    }

    @Test
    void toggleRuleActive_WhenNotFound_ShouldThrowException() {
        // Given
        when(geofenceRuleRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> geofenceRuleService.toggleRuleActive(999L, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Geofence rule not found: 999");
    }

    @Test
    void findApplicableRules_ShouldReturnRulesOrderedByPriority() {
        // Given
        GeofenceRule lowPriorityRule = GeofenceRule.builder()
                .id(2L)
                .promotion(promotion)
                .zone(zone)
                .priority(5)
                .isActive(true)
                .build();

        GeofenceRule highPriorityRule = GeofenceRule.builder()
                .id(3L)
                .promotion(promotion)
                .zone(zone)
                .priority(20)
                .isActive(true)
                .build();

        when(geofenceRuleRepository.findByZone_IdAndIsActiveTrueOrderByPriorityDesc(200L))
                .thenReturn(Arrays.asList(rule, lowPriorityRule, highPriorityRule));

        // When
        List<GeofenceRule> result = geofenceRuleService.findApplicableRules(200L);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPriority()).isEqualTo(20); // Highest priority first
        assertThat(result.get(1).getPriority()).isEqualTo(10);
        assertThat(result.get(2).getPriority()).isEqualTo(5);
    }

    @Test
    void findApplicableRules_WhenNoActiveRules_ShouldReturnEmptyList() {
        // Given
        when(geofenceRuleRepository.findByZone_IdAndIsActiveTrueOrderByPriorityDesc(200L))
                .thenReturn(Collections.emptyList());

        // When
        List<GeofenceRule> result = geofenceRuleService.findApplicableRules(200L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void countActiveRules_ShouldReturnCount() {
        // Given
        when(geofenceRuleRepository.countByCompany_IdAndIsActiveTrue(1L)).thenReturn(5L);

        // When
        Long result = geofenceRuleService.countActiveRules(1L);

        // Then
        assertThat(result).isEqualTo(5L);
        verify(geofenceRuleRepository, times(1)).countByCompany_IdAndIsActiveTrue(1L);
    }

    @Test
    void countActiveRules_WhenNoActiveRules_ShouldReturnZero() {
        // Given
        when(geofenceRuleRepository.countByCompany_IdAndIsActiveTrue(1L)).thenReturn(0L);

        // When
        Long result = geofenceRuleService.countActiveRules(1L);

        // Then
        assertThat(result).isEqualTo(0L);
    }
}
