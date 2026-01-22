package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repository.FraudAlertRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.service.FraudAlertService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudAlertServiceImplTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FraudAlertServiceImpl fraudAlertService;

    private Company company;
    private FraudRule rule;
    private FraudAlert alert;
    private User user;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .build();

        rule = FraudRule.builder()
                .id(100L)
                .name("Test Rule")
                .company(company)
                .severity(FraudSeverity.HIGH)
                .isActive(true)
                .build();

        alert = FraudAlert.builder()
                .id(1L)
                .rule(rule)
                .company(company)
                .severity(FraudSeverity.HIGH)
                .status(FraudAlertStatus.PENDING)
                .title("Suspicious Activity Detected")
                .description("Multiple failed login attempts")
                .relatedEntityType("user")
                .relatedEntityId(500L)
                .evidence(Map.of("attempts", 5, "ip", "192.168.1.1"))
                .confidenceScore(85)
                .detectedAt(now)
                .build();

        user = User.builder()
                .id(10L)
                .email("admin@example.com")
                .company(company)
                .build();
    }

    @Test
    void getAlertsByCompany_ShouldReturnAllAlertsForCompany() {
        // Given
        FraudAlert alert2 = FraudAlert.builder()
                .id(2L)
                .company(company)
                .status(FraudAlertStatus.INVESTIGATING)
                .build();

        when(fraudAlertRepository.findByCompany_Id(1L))
                .thenReturn(Arrays.asList(alert, alert2));

        // When
        List<FraudAlert> results = fraudAlertService.getAlertsByCompany(1L);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(alert, alert2);
        verify(fraudAlertRepository, times(1)).findByCompany_Id(1L);
    }

    @Test
    void getAlertsByRule_ShouldReturnAlertsForSpecificRule() {
        // Given
        when(fraudAlertRepository.findByRule_Id(100L))
                .thenReturn(Collections.singletonList(alert));

        // When
        List<FraudAlert> results = fraudAlertService.getAlertsByRule(100L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRule().getId()).isEqualTo(100L);
        verify(fraudAlertRepository, times(1)).findByRule_Id(100L);
    }

    @Test
    void getAlertsByStatus_ShouldFilterByStatus() {
        // Given
        when(fraudAlertRepository.findByCompany_IdAndStatus(1L, FraudAlertStatus.PENDING))
                .thenReturn(Collections.singletonList(alert));

        // When
        List<FraudAlert> results = fraudAlertService.getAlertsByStatus(1L, FraudAlertStatus.PENDING);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FraudAlertStatus.PENDING);
    }

    @Test
    void getAlertsBySeverity_ShouldFilterBySeverity() {
        // Given
        when(fraudAlertRepository.findByCompany_IdAndSeverity(1L, FraudSeverity.HIGH))
                .thenReturn(Collections.singletonList(alert));

        // When
        List<FraudAlert> results = fraudAlertService.getAlertsBySeverity(1L, FraudSeverity.HIGH);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSeverity()).isEqualTo(FraudSeverity.HIGH);
    }

    @Test
    void getPendingAlertsByCompany_ShouldReturnPendingOnly() {
        // Given
        when(fraudAlertRepository.findPendingAlertsByCompany(1L))
                .thenReturn(Collections.singletonList(alert));

        // When
        List<FraudAlert> results = fraudAlertService.getPendingAlertsByCompany(1L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FraudAlertStatus.PENDING);
    }

    @Test
    void getRecentAlertsByCompany_ShouldReturnLastNDays() {
        // Given
        when(fraudAlertRepository.findRecentAlertsByCompany(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(alert));

        // When
        List<FraudAlert> results = fraudAlertService.getRecentAlertsByCompany(1L, 7);

        // Then
        assertThat(results).hasSize(1);
        verify(fraudAlertRepository, times(1))
                .findRecentAlertsByCompany(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void getAlertsByRelatedEntity_ShouldFilterByEntityType() {
        // Given
        when(fraudAlertRepository.findByCompany_IdAndRelatedEntityTypeAndRelatedEntityId(1L, "user", 500L))
                .thenReturn(Collections.singletonList(alert));

        // When
        List<FraudAlert> results = fraudAlertService.getAlertsByRelatedEntity(1L, "user", 500L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRelatedEntityType()).isEqualTo("user");
        assertThat(results.get(0).getRelatedEntityId()).isEqualTo(500L);
    }

    @Test
    void getHighPriorityAlertsByCompany_ShouldReturnHighSeverityAlerts() {
        // Given
        when(fraudAlertRepository.findHighPriorityAlertsByCompany(1L))
                .thenReturn(Collections.singletonList(alert));

        // When
        List<FraudAlert> results = fraudAlertService.getHighPriorityAlertsByCompany(1L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSeverity()).isEqualTo(FraudSeverity.HIGH);
    }

    @Test
    void getAlertById_WhenFound_ShouldReturnAlert() {
        // Given
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(alert));

        // When
        Optional<FraudAlert> result = fraudAlertService.getAlertById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getTitle()).isEqualTo("Suspicious Activity Detected");
    }

    @Test
    void getAlertById_WhenNotFound_ShouldReturnEmpty() {
        // Given
        when(fraudAlertRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<FraudAlert> result = fraudAlertService.getAlertById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void createAlert_ShouldSaveNewAlert() {
        // Given
        FraudAlert newAlert = FraudAlert.builder()
                .company(company)
                .rule(rule)
                .severity(FraudSeverity.MEDIUM)
                .status(FraudAlertStatus.PENDING)
                .title("New Alert")
                .description("Test description")
                .build();

        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        // When
        FraudAlert result = fraudAlertService.createAlert(newAlert);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(fraudAlertRepository, times(1)).save(any(FraudAlert.class));
    }

    @Test
    void createAlert_WhenNoDetectedAt_ShouldSetCurrentTime() {
        // Given
        FraudAlert newAlert = FraudAlert.builder()
                .company(company)
                .rule(rule)
                .severity(FraudSeverity.MEDIUM)
                .detectedAt(null)
                .build();

        when(fraudAlertRepository.save(any(FraudAlert.class))).thenAnswer(invocation -> {
            FraudAlert savedAlert = invocation.getArgument(0);
            assertThat(savedAlert.getDetectedAt()).isNotNull();
            return alert;
        });

        // When
        FraudAlert result = fraudAlertService.createAlert(newAlert);

        // Then
        assertThat(result).isNotNull();
        verify(fraudAlertRepository, times(1)).save(newAlert);
    }

    @Test
    void createAlertFromRule_ShouldCreateAlertWithRuleDetails() {
        // Given
        Map<String, Object> evidence = Map.of("reason", "test", "score", 90);
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        // When
        FraudAlert result = fraudAlertService.createAlertFromRule(
                rule, 1L, "Rule Alert", "Generated from rule", "user", 500L, evidence, 90);

        // Then
        assertThat(result).isNotNull();
        verify(fraudAlertRepository, times(1)).save(any(FraudAlert.class));
    }

    @Test
    void updateAlertStatus_ShouldChangeStatus() {
        // Given
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        // When
        FraudAlert result = fraudAlertService.updateAlertStatus(1L, FraudAlertStatus.INVESTIGATING);

        // Then
        assertThat(result.getStatus()).isEqualTo(FraudAlertStatus.INVESTIGATING);
        verify(fraudAlertRepository, times(1)).save(alert);
    }

    @Test
    void updateAlertStatus_WhenNotFound_ShouldThrowException() {
        // Given
        when(fraudAlertRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> fraudAlertService.updateAlertStatus(999L, FraudAlertStatus.CONFIRMED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fraud alert not found with id: 999");
    }

    @Test
    void resolveAlert_ShouldUpdateStatusAndResolvedFields() {
        // Given
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        // When
        FraudAlert result = fraudAlertService.resolveAlert(1L, 10L, "Resolved after investigation");

        // Then
        assertThat(result.getStatus()).isEqualTo(FraudAlertStatus.RESOLVED);
        assertThat(result.getResolvedAt()).isNotNull();
        assertThat(result.getResolvedBy()).isEqualTo(user);
        assertThat(result.getResolutionNotes()).isEqualTo("Resolved after investigation");
        verify(fraudAlertRepository, times(1)).save(alert);
    }

    @Test
    void resolveAlert_WhenAlertNotFound_ShouldThrowException() {
        // Given
        when(fraudAlertRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> fraudAlertService.resolveAlert(999L, 10L, "notes"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fraud alert not found with id: 999");
    }

    @Test
    void resolveAlert_WhenUserNotFound_ShouldThrowException() {
        // Given
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> fraudAlertService.resolveAlert(1L, 999L, "notes"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 999");
    }

    @Test
    void updateAlert_ShouldUpdateAllFields() {
        // Given
        FraudAlert updatedData = FraudAlert.builder()
                .title("Updated Title")
                .description("Updated Description")
                .status(FraudAlertStatus.CONFIRMED)
                .severity(FraudSeverity.CRITICAL)
                .evidence(Map.of("new", "evidence"))
                .confidenceScore(95)
                .build();

        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        // When
        FraudAlert result = fraudAlertService.updateAlert(1L, updatedData);

        // Then
        assertThat(result).isNotNull();
        assertThat(alert.getTitle()).isEqualTo("Updated Title");
        assertThat(alert.getDescription()).isEqualTo("Updated Description");
        assertThat(alert.getStatus()).isEqualTo(FraudAlertStatus.CONFIRMED);
        assertThat(alert.getSeverity()).isEqualTo(FraudSeverity.CRITICAL);
        assertThat(alert.getConfidenceScore()).isEqualTo(95);
        verify(fraudAlertRepository, times(1)).save(alert);
    }

    @Test
    void updateAlert_WhenNotFound_ShouldThrowException() {
        // Given
        when(fraudAlertRepository.findById(999L)).thenReturn(Optional.empty());
        FraudAlert updatedData = FraudAlert.builder().title("New Title").build();

        // When/Then
        assertThatThrownBy(() -> fraudAlertService.updateAlert(999L, updatedData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fraud alert not found with id: 999");
    }

    @Test
    void deleteAlert_ShouldRemoveAlert() {
        // Given
        doNothing().when(fraudAlertRepository).deleteById(1L);

        // When
        fraudAlertService.deleteAlert(1L);

        // Then
        verify(fraudAlertRepository, times(1)).deleteById(1L);
    }

    @Test
    void countAlertsByStatus_ShouldReturnCount() {
        // Given
        when(fraudAlertRepository.countByCompany_IdAndStatus(1L, FraudAlertStatus.PENDING))
                .thenReturn(5L);

        // When
        long result = fraudAlertService.countAlertsByStatus(1L, FraudAlertStatus.PENDING);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    void countAlertsBySeverity_ShouldReturnCount() {
        // Given
        when(fraudAlertRepository.countByCompany_IdAndSeverity(1L, FraudSeverity.HIGH))
                .thenReturn(3L);

        // When
        long result = fraudAlertService.countAlertsBySeverity(1L, FraudSeverity.HIGH);

        // Then
        assertThat(result).isEqualTo(3L);
    }

    @Test
    void getAlertStatistics_ShouldReturnCompleteStats() {
        // Given
        when(fraudAlertRepository.countByCompany_IdAndStatus(eq(1L), any(FraudAlertStatus.class)))
                .thenReturn(5L, 3L, 2L, 1L, 4L);
        when(fraudAlertRepository.countByCompany_IdAndSeverity(eq(1L), any(FraudSeverity.class)))
                .thenReturn(2L, 5L, 4L, 1L);

        // When
        Map<String, Long> stats = fraudAlertService.getAlertStatistics(1L);

        // Then
        assertThat(stats).hasSize(9);
        assertThat(stats).containsKeys("pending", "investigating", "confirmed", "falsePositive", "resolved");
        assertThat(stats).containsKeys("low", "medium", "high", "critical");
        verify(fraudAlertRepository, times(5)).countByCompany_IdAndStatus(eq(1L), any());
        verify(fraudAlertRepository, times(4)).countByCompany_IdAndSeverity(eq(1L), any());
    }
}
