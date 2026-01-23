package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.PromotionMetrics;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repository.AdviceImpressionRepository;
import com.screenleads.backend.app.domain.repository.AdviceInteractionRepository;
import com.screenleads.backend.app.domain.repository.PromotionMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionMetricsServiceImplTest {

    @Mock
    private PromotionMetricsRepository promotionMetricsRepository;

    @Mock
    private AdviceRepository adviceRepository;

    @Mock
    private AdviceImpressionRepository adviceImpressionRepository;

    @Mock
    private AdviceInteractionRepository adviceInteractionRepository;

    @InjectMocks
    private PromotionMetricsServiceImpl promotionMetricsService;

    private Company company;
    private Advice advice1;
    private Advice advice2;
    private PromotionMetrics metrics1;
    private PromotionMetrics metrics2;
    private LocalDate testDate;
    private LocalDateTime startOfDay;
    private LocalDateTime endOfDay;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        advice1 = new Advice();
        advice1.setId(1L);
        advice1.setDescription("Advice 1");
        advice1.setCompany(company);

        advice2 = new Advice();
        advice2.setId(2L);
        advice2.setDescription("Advice 2");
        advice2.setCompany(company);

        testDate = LocalDate.of(2026, 1, 22);
        startOfDay = testDate.atStartOfDay();
        endOfDay = testDate.plusDays(1).atStartOfDay();

        metrics1 = PromotionMetrics.builder()
                .id(1L)
                .advice(advice1)
                .metricDate(testDate)
                .totalImpressions(1000L)
                .totalInteractions(100L)
                .totalConversions(10L)
                .uniqueCustomers(50L)
                .uniqueDevices(45L)
                .avgViewDurationSeconds(30.5)
                .build();

        metrics2 = PromotionMetrics.builder()
                .id(2L)
                .advice(advice2)
                .metricDate(testDate)
                .totalImpressions(500L)
                .totalInteractions(50L)
                .totalConversions(5L)
                .uniqueCustomers(25L)
                .uniqueDevices(20L)
                .avgViewDurationSeconds(25.0)
                .build();
    }

    @Test
    void calculateDailyMetrics_Success() {
        // Arrange
        List<Advice> adviceList = Arrays.asList(advice1, advice2);
        when(adviceRepository.findAll()).thenReturn(adviceList);

        // Setup for advice1
        when(adviceImpressionRepository.countByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(1000L);
        when(adviceInteractionRepository.countByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(100L);
        when(adviceInteractionRepository.countConversionsByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(10L);
        when(adviceImpressionRepository.countUniqueCustomersByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(50L);
        when(adviceImpressionRepository.countUniqueDevicesByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(45L);
        when(adviceImpressionRepository.calculateAverageDurationByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(30.5);
        when(promotionMetricsRepository.findByAdvice_IdAndMetricDate(eq(1L), any()))
                .thenReturn(Optional.empty());

        // Setup for advice2
        when(adviceImpressionRepository.countByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(500L);
        when(adviceInteractionRepository.countByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(50L);
        when(adviceInteractionRepository.countConversionsByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(5L);
        when(adviceImpressionRepository.countUniqueCustomersByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(25L);
        when(adviceImpressionRepository.countUniqueDevicesByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(20L);
        when(adviceImpressionRepository.calculateAverageDurationByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(25.0);
        when(promotionMetricsRepository.findByAdvice_IdAndMetricDate(eq(2L), any()))
                .thenReturn(Optional.empty());

        when(promotionMetricsRepository.save(any(PromotionMetrics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        promotionMetricsService.calculateDailyMetrics(testDate);

        // Assert
        verify(adviceRepository).findAll();
        verify(promotionMetricsRepository, times(2)).save(any(PromotionMetrics.class));
    }

    @Test
    void calculateDailyMetrics_UpdateExistingMetrics() {
        // Arrange
        List<Advice> adviceList = Arrays.asList(advice1);
        when(adviceRepository.findAll()).thenReturn(adviceList);

        // Setup metrics calculation
        when(adviceImpressionRepository.countByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(1500L);
        when(adviceInteractionRepository.countByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(150L);
        when(adviceInteractionRepository.countConversionsByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(15L);
        when(adviceImpressionRepository.countUniqueCustomersByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(75L);
        when(adviceImpressionRepository.countUniqueDevicesByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(70L);
        when(adviceImpressionRepository.calculateAverageDurationByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(35.0);

        // Return existing metrics
        when(promotionMetricsRepository.findByAdvice_IdAndMetricDate(eq(1L), any()))
                .thenReturn(Optional.of(metrics1));

        when(promotionMetricsRepository.save(any(PromotionMetrics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        promotionMetricsService.calculateDailyMetrics(testDate);

        // Assert
        ArgumentCaptor<PromotionMetrics> metricsCaptor = ArgumentCaptor.forClass(PromotionMetrics.class);
        verify(promotionMetricsRepository).save(metricsCaptor.capture());

        PromotionMetrics savedMetrics = metricsCaptor.getValue();
        assertEquals(1500L, savedMetrics.getTotalImpressions());
        assertEquals(150L, savedMetrics.getTotalInteractions());
        assertEquals(15L, savedMetrics.getTotalConversions());
    }

    @Test
    void calculateDailyMetrics_EmptyAdviceList() {
        // Arrange
        when(adviceRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        promotionMetricsService.calculateDailyMetrics(testDate);

        // Assert
        verify(adviceRepository).findAll();
        verify(promotionMetricsRepository, never()).save(any());
    }

    @Test
    void calculateDailyMetrics_ErrorInOneAdvice_ContinuesWithOthers() {
        // Arrange
        List<Advice> adviceList = Arrays.asList(advice1, advice2);
        when(adviceRepository.findAll()).thenReturn(adviceList);

        // Setup advice1 to throw exception
        when(adviceImpressionRepository.countByAdviceIdAndDateRange(eq(1L), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // Setup advice2 normally
        when(adviceImpressionRepository.countByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(500L);
        when(adviceInteractionRepository.countByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(50L);
        when(adviceInteractionRepository.countConversionsByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(5L);
        when(adviceImpressionRepository.countUniqueCustomersByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(25L);
        when(adviceImpressionRepository.countUniqueDevicesByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(20L);
        when(adviceImpressionRepository.calculateAverageDurationByAdviceIdAndDateRange(eq(2L), any(), any()))
                .thenReturn(25.0);
        when(promotionMetricsRepository.findByAdvice_IdAndMetricDate(eq(2L), any()))
                .thenReturn(Optional.empty());
        when(promotionMetricsRepository.save(any(PromotionMetrics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        promotionMetricsService.calculateDailyMetrics(testDate);

        // Assert - should continue and save metrics for advice2
        verify(promotionMetricsRepository, times(1)).save(any(PromotionMetrics.class));
    }

    @Test
    void calculateDailyMetricsForYesterday_Success() {
        // Arrange
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(adviceRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        promotionMetricsService.calculateDailyMetricsForYesterday();

        // Assert
        verify(adviceRepository).findAll();
    }

    @Test
    void getMetricsByAdviceAndDate_Found() {
        // Arrange
        when(promotionMetricsRepository.findByAdvice_IdAndMetricDate(1L, testDate))
                .thenReturn(Optional.of(metrics1));

        // Act
        Optional<PromotionMetrics> result = promotionMetricsService.getMetricsByAdviceAndDate(1L, testDate);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(metrics1.getId(), result.get().getId());
        verify(promotionMetricsRepository).findByAdvice_IdAndMetricDate(1L, testDate);
    }

    @Test
    void getMetricsByAdviceAndDate_NotFound() {
        // Arrange
        when(promotionMetricsRepository.findByAdvice_IdAndMetricDate(1L, testDate))
                .thenReturn(Optional.empty());

        // Act
        Optional<PromotionMetrics> result = promotionMetricsService.getMetricsByAdviceAndDate(1L, testDate);

        // Assert
        assertFalse(result.isPresent());
        verify(promotionMetricsRepository).findByAdvice_IdAndMetricDate(1L, testDate);
    }

    @Test
    void getMetricsByAdvice_Success() {
        // Arrange
        List<PromotionMetrics> metricsList = Arrays.asList(metrics1, metrics2);
        when(promotionMetricsRepository.findByAdvice_IdOrderByMetricDateDesc(1L))
                .thenReturn(metricsList);

        // Act
        List<PromotionMetrics> result = promotionMetricsService.getMetricsByAdvice(1L);

        // Assert
        assertEquals(2, result.size());
        verify(promotionMetricsRepository).findByAdvice_IdOrderByMetricDateDesc(1L);
    }

    @Test
    void getMetricsByAdvice_EmptyList() {
        // Arrange
        when(promotionMetricsRepository.findByAdvice_IdOrderByMetricDateDesc(1L))
                .thenReturn(Arrays.asList());

        // Act
        List<PromotionMetrics> result = promotionMetricsService.getMetricsByAdvice(1L);

        // Assert
        assertTrue(result.isEmpty());
        verify(promotionMetricsRepository).findByAdvice_IdOrderByMetricDateDesc(1L);
    }

    @Test
    void getMetricsByAdviceAndDateRange_Success() {
        // Arrange
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        List<PromotionMetrics> metricsList = Arrays.asList(metrics1, metrics2);
        when(promotionMetricsRepository.findByAdviceIdAndDateRange(1L, startDate, endDate))
                .thenReturn(metricsList);

        // Act
        List<PromotionMetrics> result = promotionMetricsService.getMetricsByAdviceAndDateRange(1L, startDate, endDate);

        // Assert
        assertEquals(2, result.size());
        verify(promotionMetricsRepository).findByAdviceIdAndDateRange(1L, startDate, endDate);
    }

    @Test
    void getMetricsByAdviceAndDateRange_EmptyList() {
        // Arrange
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        when(promotionMetricsRepository.findByAdviceIdAndDateRange(1L, startDate, endDate))
                .thenReturn(Arrays.asList());

        // Act
        List<PromotionMetrics> result = promotionMetricsService.getMetricsByAdviceAndDateRange(1L, startDate, endDate);

        // Assert
        assertTrue(result.isEmpty());
        verify(promotionMetricsRepository).findByAdviceIdAndDateRange(1L, startDate, endDate);
    }

    @Test
    void getMetricsByDate_Success() {
        // Arrange
        List<PromotionMetrics> metricsList = Arrays.asList(metrics1, metrics2);
        when(promotionMetricsRepository.findByMetricDateOrderByTotalImpressionsDesc(testDate))
                .thenReturn(metricsList);

        // Act
        List<PromotionMetrics> result = promotionMetricsService.getMetricsByDate(testDate);

        // Assert
        assertEquals(2, result.size());
        verify(promotionMetricsRepository).findByMetricDateOrderByTotalImpressionsDesc(testDate);
    }

    @Test
    void getMetricsByDate_EmptyList() {
        // Arrange
        when(promotionMetricsRepository.findByMetricDateOrderByTotalImpressionsDesc(testDate))
                .thenReturn(Arrays.asList());

        // Act
        List<PromotionMetrics> result = promotionMetricsService.getMetricsByDate(testDate);

        // Assert
        assertTrue(result.isEmpty());
        verify(promotionMetricsRepository).findByMetricDateOrderByTotalImpressionsDesc(testDate);
    }

    @Test
    void getTopPerformingByConversionRate_Success() {
        // Arrange
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        int limit = 10;
        List<PromotionMetrics> metricsList = Arrays.asList(metrics1, metrics2);
        when(promotionMetricsRepository.findTopPerformingByConversionRate(startDate, endDate))
                .thenReturn(metricsList);

        // Act
        List<PromotionMetrics> result = promotionMetricsService.getTopPerformingByConversionRate(startDate, endDate,
                limit);

        // Assert
        assertEquals(2, result.size());
        verify(promotionMetricsRepository).findTopPerformingByConversionRate(startDate, endDate);
    }

    @Test
    void getTopPerformingByConversionRate_LimitApplied() {
        // Arrange
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        int limit = 1;
        List<PromotionMetrics> metricsList = Arrays.asList(metrics1, metrics2);
        when(promotionMetricsRepository.findTopPerformingByConversionRate(startDate, endDate))
                .thenReturn(metricsList);

        // Act
        List<PromotionMetrics> result = promotionMetricsService.getTopPerformingByConversionRate(startDate, endDate,
                limit);

        // Assert
        assertEquals(1, result.size());
        verify(promotionMetricsRepository).findTopPerformingByConversionRate(startDate, endDate);
    }

    @Test
    void getTopPerformingByConversions_Success() {
        // Arrange
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        int limit = 10;
        List<PromotionMetrics> metricsList = Arrays.asList(metrics1, metrics2);
        when(promotionMetricsRepository.findTopPerformingByConversions(startDate, endDate))
                .thenReturn(metricsList);

        // Act
        List<PromotionMetrics> result = promotionMetricsService.getTopPerformingByConversions(startDate, endDate,
                limit);

        // Assert
        assertEquals(2, result.size());
        verify(promotionMetricsRepository).findTopPerformingByConversions(startDate, endDate);
    }

    @Test
    void getTopPerformingByConversions_LimitApplied() {
        // Arrange
        LocalDate startDate = testDate.minusDays(7);
        LocalDate endDate = testDate;
        int limit = 1;
        List<PromotionMetrics> metricsList = Arrays.asList(metrics1, metrics2);
        when(promotionMetricsRepository.findTopPerformingByConversions(startDate, endDate))
                .thenReturn(metricsList);

        // Act
        List<PromotionMetrics> result = promotionMetricsService.getTopPerformingByConversions(startDate, endDate,
                limit);

        // Assert
        assertEquals(1, result.size());
        verify(promotionMetricsRepository).findTopPerformingByConversions(startDate, endDate);
    }

    @Test
    void getTotalImpressionsByAdvice_Success() {
        // Arrange
        when(promotionMetricsRepository.getTotalImpressionsByAdviceId(1L))
                .thenReturn(5000L);

        // Act
        Long result = promotionMetricsService.getTotalImpressionsByAdvice(1L);

        // Assert
        assertEquals(5000L, result);
        verify(promotionMetricsRepository).getTotalImpressionsByAdviceId(1L);
    }

    @Test
    void getTotalImpressionsByAdvice_Zero() {
        // Arrange
        when(promotionMetricsRepository.getTotalImpressionsByAdviceId(1L))
                .thenReturn(0L);

        // Act
        Long result = promotionMetricsService.getTotalImpressionsByAdvice(1L);

        // Assert
        assertEquals(0L, result);
        verify(promotionMetricsRepository).getTotalImpressionsByAdviceId(1L);
    }

    @Test
    void getTotalConversionsByAdvice_Success() {
        // Arrange
        when(promotionMetricsRepository.getTotalConversionsByAdviceId(1L))
                .thenReturn(150L);

        // Act
        Long result = promotionMetricsService.getTotalConversionsByAdvice(1L);

        // Assert
        assertEquals(150L, result);
        verify(promotionMetricsRepository).getTotalConversionsByAdviceId(1L);
    }

    @Test
    void getTotalConversionsByAdvice_Zero() {
        // Arrange
        when(promotionMetricsRepository.getTotalConversionsByAdviceId(1L))
                .thenReturn(0L);

        // Act
        Long result = promotionMetricsService.getTotalConversionsByAdvice(1L);

        // Assert
        assertEquals(0L, result);
        verify(promotionMetricsRepository).getTotalConversionsByAdviceId(1L);
    }

    @Test
    void metricsExistForDate_True() {
        // Arrange
        when(promotionMetricsRepository.existsByMetricDate(testDate))
                .thenReturn(true);

        // Act
        boolean result = promotionMetricsService.metricsExistForDate(testDate);

        // Assert
        assertTrue(result);
        verify(promotionMetricsRepository).existsByMetricDate(testDate);
    }

    @Test
    void metricsExistForDate_False() {
        // Arrange
        when(promotionMetricsRepository.existsByMetricDate(testDate))
                .thenReturn(false);

        // Act
        boolean result = promotionMetricsService.metricsExistForDate(testDate);

        // Assert
        assertFalse(result);
        verify(promotionMetricsRepository).existsByMetricDate(testDate);
    }

    @Test
    void getLatestMetricDate_Found() {
        // Arrange
        when(promotionMetricsRepository.findLatestMetricDate())
                .thenReturn(Optional.of(testDate));

        // Act
        Optional<LocalDate> result = promotionMetricsService.getLatestMetricDate();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testDate, result.get());
        verify(promotionMetricsRepository).findLatestMetricDate();
    }

    @Test
    void getLatestMetricDate_NotFound() {
        // Arrange
        when(promotionMetricsRepository.findLatestMetricDate())
                .thenReturn(Optional.empty());

        // Act
        Optional<LocalDate> result = promotionMetricsService.getLatestMetricDate();

        // Assert
        assertFalse(result.isPresent());
        verify(promotionMetricsRepository).findLatestMetricDate();
    }
}
