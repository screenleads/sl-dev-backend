package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repository.AdviceInteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdviceInteractionServiceImplTest {

    @Mock
    private AdviceInteractionRepository adviceInteractionRepository;

    @InjectMocks
    private AdviceInteractionServiceImpl adviceInteractionService;

    private Company company;
    private Advice advice;
    private Device device;
    private AdviceImpression impression;
    private AdviceInteraction interaction1;
    private AdviceInteraction interaction2;
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

        impression = AdviceImpression.builder()
                .id(1L)
                .advice(advice)
                .device(device)
                .sessionId("session-123")
                .build();

        testDate = LocalDateTime.of(2026, 1, 22, 10, 0);

        interaction1 = AdviceInteraction.builder()
                .id(1L)
                .impression(impression)
                .type(InteractionType.VIEW_DETAILS)
                .timestamp(testDate)
                .durationSeconds(10)
                .isConversion(false)
                .build();

        interaction2 = AdviceInteraction.builder()
                .id(2L)
                .impression(impression)
                .type(InteractionType.REDEEM_START)
                .timestamp(testDate.plusMinutes(5))
                .durationSeconds(30)
                .isConversion(true)
                .build();
    }

    @Test
    void createInteraction_Success() {
        // Arrange
        when(adviceInteractionRepository.save(any(AdviceInteraction.class)))
                .thenReturn(interaction1);

        // Act
        AdviceInteraction result = adviceInteractionService.createInteraction(interaction1);

        // Assert
        assertNotNull(result);
        assertEquals(interaction1.getId(), result.getId());
        assertEquals(InteractionType.VIEW_DETAILS, result.getType());
        verify(adviceInteractionRepository).save(interaction1);
    }

    @Test
    void createInteraction_WithAllFields() {
        // Arrange
        AdviceInteraction fullInteraction = AdviceInteraction.builder()
                .impression(impression)
                .type(InteractionType.REDEEM_COMPLETE)
                .timestamp(testDate)
                .durationSeconds(60)
                .isConversion(true)
                .details(Map.of("product_id", 123))
                .build();

        when(adviceInteractionRepository.save(any(AdviceInteraction.class)))
                .thenReturn(fullInteraction);

        // Act
        AdviceInteraction result = adviceInteractionService.createInteraction(fullInteraction);

        // Assert
        assertNotNull(result);
        assertEquals(InteractionType.REDEEM_COMPLETE, result.getType());
        assertTrue(result.getIsConversion());
        assertEquals(60, result.getDurationSeconds());
        assertNotNull(result.getDetails());
        verify(adviceInteractionRepository).save(fullInteraction);
    }

    @Test
    void getInteractionById_Found() {
        // Arrange
        when(adviceInteractionRepository.findById(1L))
                .thenReturn(Optional.of(interaction1));

        // Act
        Optional<AdviceInteraction> result = adviceInteractionService.getInteractionById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(interaction1.getId(), result.get().getId());
        verify(adviceInteractionRepository).findById(1L);
    }

    @Test
    void getInteractionById_NotFound() {
        // Arrange
        when(adviceInteractionRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        Optional<AdviceInteraction> result = adviceInteractionService.getInteractionById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(adviceInteractionRepository).findById(999L);
    }

    @Test
    void getInteractionsByImpressionId_Success() {
        // Arrange
        List<AdviceInteraction> interactions = Arrays.asList(interaction1, interaction2);
        when(adviceInteractionRepository.findByImpression_Id(1L))
                .thenReturn(interactions);

        // Act
        List<AdviceInteraction> result = adviceInteractionService.getInteractionsByImpressionId(1L);

        // Assert
        assertEquals(2, result.size());
        assertEquals(interaction1.getId(), result.get(0).getId());
        assertEquals(interaction2.getId(), result.get(1).getId());
        verify(adviceInteractionRepository).findByImpression_Id(1L);
    }

    @Test
    void getInteractionsByImpressionId_EmptyList() {
        // Arrange
        when(adviceInteractionRepository.findByImpression_Id(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<AdviceInteraction> result = adviceInteractionService.getInteractionsByImpressionId(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(adviceInteractionRepository).findByImpression_Id(999L);
    }

    @Test
    void getInteractionsByCustomerId_Success() {
        // Arrange
        List<AdviceInteraction> interactions = Arrays.asList(interaction1, interaction2);
        when(adviceInteractionRepository.findByCustomer_Id(100L))
                .thenReturn(interactions);

        // Act
        List<AdviceInteraction> result = adviceInteractionService.getInteractionsByCustomerId(100L);

        // Assert
        assertEquals(2, result.size());
        verify(adviceInteractionRepository).findByCustomer_Id(100L);
    }

    @Test
    void getInteractionsByCustomerId_EmptyList() {
        // Arrange
        when(adviceInteractionRepository.findByCustomer_Id(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<AdviceInteraction> result = adviceInteractionService.getInteractionsByCustomerId(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(adviceInteractionRepository).findByCustomer_Id(999L);
    }

    @Test
    void getInteractionsByType_ViewDetails() {
        // Arrange
        List<AdviceInteraction> interactions = Arrays.asList(interaction1);
        when(adviceInteractionRepository.findByType(InteractionType.VIEW_DETAILS))
                .thenReturn(interactions);

        // Act
        List<AdviceInteraction> result = adviceInteractionService.getInteractionsByType(InteractionType.VIEW_DETAILS);

        // Assert
        assertEquals(1, result.size());
        assertEquals(InteractionType.VIEW_DETAILS, result.get(0).getType());
        verify(adviceInteractionRepository).findByType(InteractionType.VIEW_DETAILS);
    }

    @Test
    void getInteractionsByType_EmptyList() {
        // Arrange
        when(adviceInteractionRepository.findByType(InteractionType.SHARE))
                .thenReturn(Arrays.asList());

        // Act
        List<AdviceInteraction> result = adviceInteractionService.getInteractionsByType(InteractionType.SHARE);

        // Assert
        assertTrue(result.isEmpty());
        verify(adviceInteractionRepository).findByType(InteractionType.SHARE);
    }

    @Test
    void getInteractionsByAdviceId_Success() {
        // Arrange
        List<AdviceInteraction> interactions = Arrays.asList(interaction1, interaction2);
        when(adviceInteractionRepository.findByAdviceId(1L))
                .thenReturn(interactions);

        // Act
        List<AdviceInteraction> result = adviceInteractionService.getInteractionsByAdviceId(1L);

        // Assert
        assertEquals(2, result.size());
        verify(adviceInteractionRepository).findByAdviceId(1L);
    }

    @Test
    void getInteractionsByAdviceId_EmptyList() {
        // Arrange
        when(adviceInteractionRepository.findByAdviceId(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<AdviceInteraction> result = adviceInteractionService.getInteractionsByAdviceId(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(adviceInteractionRepository).findByAdviceId(999L);
    }

    @Test
    void getInteractionsByAdviceIdAndDateRange_Success() {
        // Arrange
        LocalDateTime startDate = testDate.minusHours(1);
        LocalDateTime endDate = testDate.plusHours(1);
        List<AdviceInteraction> interactions = Arrays.asList(interaction1, interaction2);
        when(adviceInteractionRepository.findByAdviceIdAndDateRange(1L, startDate, endDate))
                .thenReturn(interactions);

        // Act
        List<AdviceInteraction> result = adviceInteractionService.getInteractionsByAdviceIdAndDateRange(
                1L, startDate, endDate);

        // Assert
        assertEquals(2, result.size());
        verify(adviceInteractionRepository).findByAdviceIdAndDateRange(1L, startDate, endDate);
    }

    @Test
    void getInteractionsByAdviceIdAndDateRange_EmptyList() {
        // Arrange
        LocalDateTime startDate = testDate.minusDays(10);
        LocalDateTime endDate = testDate.minusDays(9);
        when(adviceInteractionRepository.findByAdviceIdAndDateRange(1L, startDate, endDate))
                .thenReturn(Arrays.asList());

        // Act
        List<AdviceInteraction> result = adviceInteractionService.getInteractionsByAdviceIdAndDateRange(
                1L, startDate, endDate);

        // Assert
        assertTrue(result.isEmpty());
        verify(adviceInteractionRepository).findByAdviceIdAndDateRange(1L, startDate, endDate);
    }

    @Test
    void countInteractionsByAdviceId_Success() {
        // Arrange
        when(adviceInteractionRepository.countByAdviceId(1L))
                .thenReturn(250L);

        // Act
        Long result = adviceInteractionService.countInteractionsByAdviceId(1L);

        // Assert
        assertEquals(250L, result);
        verify(adviceInteractionRepository).countByAdviceId(1L);
    }

    @Test
    void countInteractionsByAdviceId_Zero() {
        // Arrange
        when(adviceInteractionRepository.countByAdviceId(999L))
                .thenReturn(0L);

        // Act
        Long result = adviceInteractionService.countInteractionsByAdviceId(999L);

        // Assert
        assertEquals(0L, result);
        verify(adviceInteractionRepository).countByAdviceId(999L);
    }

    @Test
    void countInteractionsByAdviceIdAndType_ViewDetails() {
        // Arrange
        when(adviceInteractionRepository.countByAdviceIdAndType(1L, InteractionType.VIEW_DETAILS))
                .thenReturn(100L);

        // Act
        Long result = adviceInteractionService.countInteractionsByAdviceIdAndType(1L, InteractionType.VIEW_DETAILS);

        // Assert
        assertEquals(100L, result);
        verify(adviceInteractionRepository).countByAdviceIdAndType(1L, InteractionType.VIEW_DETAILS);
    }

    @Test
    void countInteractionsByAdviceIdAndType_Zero() {
        // Arrange
        when(adviceInteractionRepository.countByAdviceIdAndType(999L, InteractionType.SHARE))
                .thenReturn(0L);

        // Act
        Long result = adviceInteractionService.countInteractionsByAdviceIdAndType(999L, InteractionType.SHARE);

        // Assert
        assertEquals(0L, result);
        verify(adviceInteractionRepository).countByAdviceIdAndType(999L, InteractionType.SHARE);
    }

    @Test
    void countConversionsByAdviceId_Success() {
        // Arrange
        when(adviceInteractionRepository.countConversionsByAdviceId(1L))
                .thenReturn(50L);

        // Act
        Long result = adviceInteractionService.countConversionsByAdviceId(1L);

        // Assert
        assertEquals(50L, result);
        verify(adviceInteractionRepository).countConversionsByAdviceId(1L);
    }

    @Test
    void countConversionsByAdviceId_Zero() {
        // Arrange
        when(adviceInteractionRepository.countConversionsByAdviceId(999L))
                .thenReturn(0L);

        // Act
        Long result = adviceInteractionService.countConversionsByAdviceId(999L);

        // Assert
        assertEquals(0L, result);
        verify(adviceInteractionRepository).countConversionsByAdviceId(999L);
    }

    @Test
    void getUniqueCustomerCountByAdviceId_Success() {
        // Arrange
        List<Long> customerIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        when(adviceInteractionRepository.findUniqueCustomerIdsByAdviceId(1L))
                .thenReturn(customerIds);

        // Act
        Long result = adviceInteractionService.getUniqueCustomerCountByAdviceId(1L);

        // Assert
        assertEquals(5L, result);
        verify(adviceInteractionRepository).findUniqueCustomerIdsByAdviceId(1L);
    }

    @Test
    void getUniqueCustomerCountByAdviceId_EmptyList() {
        // Arrange
        when(adviceInteractionRepository.findUniqueCustomerIdsByAdviceId(999L))
                .thenReturn(Arrays.asList());

        // Act
        Long result = adviceInteractionService.getUniqueCustomerCountByAdviceId(999L);

        // Assert
        assertEquals(0L, result);
        verify(adviceInteractionRepository).findUniqueCustomerIdsByAdviceId(999L);
    }

    @Test
    void calculateAverageDurationByAdviceId_Success() {
        // Arrange
        when(adviceInteractionRepository.calculateAverageDurationByAdviceId(1L))
                .thenReturn(42.5);

        // Act
        Double result = adviceInteractionService.calculateAverageDurationByAdviceId(1L);

        // Assert
        assertEquals(42.5, result);
        verify(adviceInteractionRepository).calculateAverageDurationByAdviceId(1L);
    }

    @Test
    void calculateAverageDurationByAdviceId_Null() {
        // Arrange
        when(adviceInteractionRepository.calculateAverageDurationByAdviceId(999L))
                .thenReturn(null);

        // Act
        Double result = adviceInteractionService.calculateAverageDurationByAdviceId(999L);

        // Assert
        assertEquals(0.0, result);
        verify(adviceInteractionRepository).calculateAverageDurationByAdviceId(999L);
    }

    @Test
    void calculateAverageDurationByAdviceId_Zero() {
        // Arrange
        when(adviceInteractionRepository.calculateAverageDurationByAdviceId(1L))
                .thenReturn(0.0);

        // Act
        Double result = adviceInteractionService.calculateAverageDurationByAdviceId(1L);

        // Assert
        assertEquals(0.0, result);
        verify(adviceInteractionRepository).calculateAverageDurationByAdviceId(1L);
    }
}
