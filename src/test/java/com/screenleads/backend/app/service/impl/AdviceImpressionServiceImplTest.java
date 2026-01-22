package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.AdviceImpression;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.repository.AdviceImpressionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdviceImpressionServiceImplTest {

    @Mock
    private AdviceImpressionRepository adviceImpressionRepository;

    @InjectMocks
    private AdviceImpressionServiceImpl adviceImpressionService;

    private Company company;
    private Advice advice;
    private Device device;
    private AdviceImpression impression1;
    private AdviceImpression impression2;
    private LocalDateTime testDate;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        advice = new Advice();
        advice.setId(1L);
        advice.setDescription("Test Advice");
        advice.setCompany(company);

        device = new Device();
        device.setId(1L);

        testDate = LocalDateTime.of(2026, 1, 22, 10, 0);

        impression1 = AdviceImpression.builder()
                .id(1L)
                .advice(advice)
                .device(device)
                .sessionId("session-123")
                .timestamp(testDate)
                .durationSeconds(30)
                .wasInteractive(true)
                .build();

        impression2 = AdviceImpression.builder()
                .id(2L)
                .advice(advice)
                .device(device)
                .sessionId("session-456")
                .timestamp(testDate.plusMinutes(10))
                .durationSeconds(45)
                .wasInteractive(false)
                .build();
    }

    @Test
    void createImpression_Success() {
        // Arrange
        when(adviceImpressionRepository.save(any(AdviceImpression.class)))
                .thenReturn(impression1);

        // Act
        AdviceImpression result = adviceImpressionService.createImpression(impression1);

        // Assert
        assertNotNull(result);
        assertEquals(impression1.getId(), result.getId());
        assertEquals(impression1.getSessionId(), result.getSessionId());
        verify(adviceImpressionRepository).save(impression1);
    }

    @Test
    void createImpression_WithAllFields() {
        // Arrange
        AdviceImpression fullImpression = AdviceImpression.builder()
                .advice(advice)
                .device(device)
                .sessionId("full-session")
                .timestamp(testDate)
                .durationSeconds(60)
                .wasInteractive(true)
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        when(adviceImpressionRepository.save(any(AdviceImpression.class)))
                .thenReturn(fullImpression);

        // Act
        AdviceImpression result = adviceImpressionService.createImpression(fullImpression);

        // Assert
        assertNotNull(result);
        assertEquals(60, result.getDurationSeconds());
        assertTrue(result.getWasInteractive());
        assertEquals(40.7128, result.getLatitude());
        assertEquals(-74.0060, result.getLongitude());
        verify(adviceImpressionRepository).save(fullImpression);
    }

    @Test
    void getImpressionById_Found() {
        // Arrange
        when(adviceImpressionRepository.findById(1L))
                .thenReturn(Optional.of(impression1));

        // Act
        Optional<AdviceImpression> result = adviceImpressionService.getImpressionById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(impression1.getId(), result.get().getId());
        verify(adviceImpressionRepository).findById(1L);
    }

    @Test
    void getImpressionById_NotFound() {
        // Arrange
        when(adviceImpressionRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        Optional<AdviceImpression> result = adviceImpressionService.getImpressionById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(adviceImpressionRepository).findById(999L);
    }

    @Test
    void getImpressionsByAdviceId_Success() {
        // Arrange
        List<AdviceImpression> impressions = Arrays.asList(impression1, impression2);
        when(adviceImpressionRepository.findByAdvice_Id(1L))
                .thenReturn(impressions);

        // Act
        List<AdviceImpression> result = adviceImpressionService.getImpressionsByAdviceId(1L);

        // Assert
        assertEquals(2, result.size());
        assertEquals(impression1.getId(), result.get(0).getId());
        assertEquals(impression2.getId(), result.get(1).getId());
        verify(adviceImpressionRepository).findByAdvice_Id(1L);
    }

    @Test
    void getImpressionsByAdviceId_EmptyList() {
        // Arrange
        when(adviceImpressionRepository.findByAdvice_Id(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<AdviceImpression> result = adviceImpressionService.getImpressionsByAdviceId(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(adviceImpressionRepository).findByAdvice_Id(999L);
    }

    @Test
    void getImpressionsByDeviceId_Success() {
        // Arrange
        List<AdviceImpression> impressions = Arrays.asList(impression1, impression2);
        when(adviceImpressionRepository.findByDevice_Id(1L))
                .thenReturn(impressions);

        // Act
        List<AdviceImpression> result = adviceImpressionService.getImpressionsByDeviceId(1L);

        // Assert
        assertEquals(2, result.size());
        verify(adviceImpressionRepository).findByDevice_Id(1L);
    }

    @Test
    void getImpressionsByDeviceId_EmptyList() {
        // Arrange
        when(adviceImpressionRepository.findByDevice_Id(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<AdviceImpression> result = adviceImpressionService.getImpressionsByDeviceId(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(adviceImpressionRepository).findByDevice_Id(999L);
    }

    @Test
    void getImpressionsByCustomerId_Success() {
        // Arrange
        List<AdviceImpression> impressions = Arrays.asList(impression1, impression2);
        when(adviceImpressionRepository.findByCustomer_Id(100L))
                .thenReturn(impressions);

        // Act
        List<AdviceImpression> result = adviceImpressionService.getImpressionsByCustomerId(100L);

        // Assert
        assertEquals(2, result.size());
        verify(adviceImpressionRepository).findByCustomer_Id(100L);
    }

    @Test
    void getImpressionsByCustomerId_EmptyList() {
        // Arrange
        when(adviceImpressionRepository.findByCustomer_Id(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<AdviceImpression> result = adviceImpressionService.getImpressionsByCustomerId(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(adviceImpressionRepository).findByCustomer_Id(999L);
    }

    @Test
    void getImpressionsBySessionId_Success() {
        // Arrange
        List<AdviceImpression> impressions = Arrays.asList(impression1);
        when(adviceImpressionRepository.findBySessionId("session-123"))
                .thenReturn(impressions);

        // Act
        List<AdviceImpression> result = adviceImpressionService.getImpressionsBySessionId("session-123");

        // Assert
        assertEquals(1, result.size());
        assertEquals("session-123", result.get(0).getSessionId());
        verify(adviceImpressionRepository).findBySessionId("session-123");
    }

    @Test
    void getImpressionsBySessionId_EmptyList() {
        // Arrange
        when(adviceImpressionRepository.findBySessionId("nonexistent"))
                .thenReturn(Arrays.asList());

        // Act
        List<AdviceImpression> result = adviceImpressionService.getImpressionsBySessionId("nonexistent");

        // Assert
        assertTrue(result.isEmpty());
        verify(adviceImpressionRepository).findBySessionId("nonexistent");
    }

    @Test
    void getImpressionsByAdviceIdAndDateRange_Success() {
        // Arrange
        LocalDateTime startDate = testDate.minusHours(1);
        LocalDateTime endDate = testDate.plusHours(1);
        List<AdviceImpression> impressions = Arrays.asList(impression1, impression2);
        when(adviceImpressionRepository.findByAdviceIdAndDateRange(1L, startDate, endDate))
                .thenReturn(impressions);

        // Act
        List<AdviceImpression> result = adviceImpressionService.getImpressionsByAdviceIdAndDateRange(
                1L, startDate, endDate);

        // Assert
        assertEquals(2, result.size());
        verify(adviceImpressionRepository).findByAdviceIdAndDateRange(1L, startDate, endDate);
    }

    @Test
    void getImpressionsByAdviceIdAndDateRange_EmptyList() {
        // Arrange
        LocalDateTime startDate = testDate.minusDays(10);
        LocalDateTime endDate = testDate.minusDays(9);
        when(adviceImpressionRepository.findByAdviceIdAndDateRange(1L, startDate, endDate))
                .thenReturn(Arrays.asList());

        // Act
        List<AdviceImpression> result = adviceImpressionService.getImpressionsByAdviceIdAndDateRange(
                1L, startDate, endDate);

        // Assert
        assertTrue(result.isEmpty());
        verify(adviceImpressionRepository).findByAdviceIdAndDateRange(1L, startDate, endDate);
    }

    @Test
    void countImpressionsByAdviceId_Success() {
        // Arrange
        when(adviceImpressionRepository.countByAdvice_Id(1L))
                .thenReturn(150L);

        // Act
        Long result = adviceImpressionService.countImpressionsByAdviceId(1L);

        // Assert
        assertEquals(150L, result);
        verify(adviceImpressionRepository).countByAdvice_Id(1L);
    }

    @Test
    void countImpressionsByAdviceId_Zero() {
        // Arrange
        when(adviceImpressionRepository.countByAdvice_Id(999L))
                .thenReturn(0L);

        // Act
        Long result = adviceImpressionService.countImpressionsByAdviceId(999L);

        // Assert
        assertEquals(0L, result);
        verify(adviceImpressionRepository).countByAdvice_Id(999L);
    }

    @Test
    void countInteractiveImpressionsByAdviceId_Success() {
        // Arrange
        when(adviceImpressionRepository.countByAdvice_IdAndWasInteractiveTrue(1L))
                .thenReturn(75L);

        // Act
        Long result = adviceImpressionService.countInteractiveImpressionsByAdviceId(1L);

        // Assert
        assertEquals(75L, result);
        verify(adviceImpressionRepository).countByAdvice_IdAndWasInteractiveTrue(1L);
    }

    @Test
    void countInteractiveImpressionsByAdviceId_Zero() {
        // Arrange
        when(adviceImpressionRepository.countByAdvice_IdAndWasInteractiveTrue(999L))
                .thenReturn(0L);

        // Act
        Long result = adviceImpressionService.countInteractiveImpressionsByAdviceId(999L);

        // Assert
        assertEquals(0L, result);
        verify(adviceImpressionRepository).countByAdvice_IdAndWasInteractiveTrue(999L);
    }

    @Test
    void getUniqueCustomerCountByAdviceId_Success() {
        // Arrange
        List<Long> customerIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        when(adviceImpressionRepository.findUniqueCustomerIdsByAdviceId(1L))
                .thenReturn(customerIds);

        // Act
        Long result = adviceImpressionService.getUniqueCustomerCountByAdviceId(1L);

        // Assert
        assertEquals(5L, result);
        verify(adviceImpressionRepository).findUniqueCustomerIdsByAdviceId(1L);
    }

    @Test
    void getUniqueCustomerCountByAdviceId_EmptyList() {
        // Arrange
        when(adviceImpressionRepository.findUniqueCustomerIdsByAdviceId(999L))
                .thenReturn(Arrays.asList());

        // Act
        Long result = adviceImpressionService.getUniqueCustomerCountByAdviceId(999L);

        // Assert
        assertEquals(0L, result);
        verify(adviceImpressionRepository).findUniqueCustomerIdsByAdviceId(999L);
    }

    @Test
    void calculateAverageDurationByAdviceId_Success() {
        // Arrange
        when(adviceImpressionRepository.calculateAverageDurationByAdviceId(1L))
                .thenReturn(45.5);

        // Act
        Double result = adviceImpressionService.calculateAverageDurationByAdviceId(1L);

        // Assert
        assertEquals(45.5, result);
        verify(adviceImpressionRepository).calculateAverageDurationByAdviceId(1L);
    }

    @Test
    void calculateAverageDurationByAdviceId_Null() {
        // Arrange
        when(adviceImpressionRepository.calculateAverageDurationByAdviceId(999L))
                .thenReturn(null);

        // Act
        Double result = adviceImpressionService.calculateAverageDurationByAdviceId(999L);

        // Assert
        assertEquals(0.0, result);
        verify(adviceImpressionRepository).calculateAverageDurationByAdviceId(999L);
    }

    @Test
    void calculateAverageDurationByAdviceId_Zero() {
        // Arrange
        when(adviceImpressionRepository.calculateAverageDurationByAdviceId(1L))
                .thenReturn(0.0);

        // Act
        Double result = adviceImpressionService.calculateAverageDurationByAdviceId(1L);

        // Assert
        assertEquals(0.0, result);
        verify(adviceImpressionRepository).calculateAverageDurationByAdviceId(1L);
    }
}
