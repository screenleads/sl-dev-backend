package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.web.dto.PromotionDTO;
import com.screenleads.backend.app.web.dto.PromotionLeadDTO;
import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.PromotionLeadRepository;
import com.screenleads.backend.app.domain.repositories.PromotionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionServiceImpl Unit Tests")
class PromotionServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionLeadRepository promotionLeadRepository;

    @Mock
    private StripeBillingService billingService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    private Promotion testPromotion;
    private PromotionDTO testPromotionDTO;
    private PromotionLead testLead;

    @BeforeEach
    void setUp() {
        testPromotion = Promotion.builder()
                .id(1L)
                .name("Test Promotion")
                .description("Test Description")
                .startAt(Instant.now())
                .leadLimitType(LeadLimitType.NO_LIMIT)
                .build();

        testPromotionDTO = new PromotionDTO(
                1L, "https://example.com/legal", "https://example.com",
                "Test Description", "<h1>Template</h1>", 
                LeadLimitType.NO_LIMIT, LeadIdentifierType.EMAIL);

        testLead = PromotionLead.builder()
                .id(1L)
                .promotion(testPromotion)
                .identifierType(LeadIdentifierType.EMAIL)
                .identifier("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    @DisplayName("getAllPromotions should return all promotions as DTOs")
    void whenGetAllPromotions_thenReturnsAllPromotions() {
        // Arrange
        when(promotionRepository.findAll()).thenReturn(List.of(testPromotion));
        when(objectMapper.convertValue(any(Promotion.class), eq(PromotionDTO.class))).thenReturn(testPromotionDTO);

        // Act
        List<PromotionDTO> result = promotionService.getAllPromotions();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).description()).isEqualTo("Test Description");
        verify(promotionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getPromotionById should return promotion when found")
    void whenGetPromotionByIdExists_thenReturnsPromotion() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(objectMapper.convertValue(any(Promotion.class), eq(PromotionDTO.class))).thenReturn(testPromotionDTO);

        // Act
        PromotionDTO result = promotionService.getPromotionById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.description()).isEqualTo("Test Description");
    }

    @Test
    @DisplayName("getPromotionById should throw exception when not found")
    void whenGetPromotionByIdNotExists_thenThrowsException() {
        // Arrange
        when(promotionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> promotionService.getPromotionById(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("savePromotion should create and return new promotion")
    void whenSavePromotion_thenCreatesAndReturns() {
        // Arrange
        when(objectMapper.convertValue(any(PromotionDTO.class), eq(Promotion.class))).thenReturn(testPromotion);
        when(promotionRepository.save(any(Promotion.class))).thenReturn(testPromotion);
        when(objectMapper.convertValue(any(Promotion.class), eq(PromotionDTO.class))).thenReturn(testPromotionDTO);

        // Act
        PromotionDTO result = promotionService.savePromotion(testPromotionDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(promotionRepository, times(1)).save(any(Promotion.class));
    }

    @Test
    @DisplayName("updatePromotion should merge changes and save")
    void whenUpdatePromotion_thenMergesAndSaves() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(objectMapper.convertValue(any(PromotionDTO.class), eq(Promotion.class))).thenReturn(testPromotion);
        when(objectMapper.convertValue(any(Promotion.class), eq(PromotionDTO.class))).thenReturn(testPromotionDTO);

        // Act
        PromotionDTO result = promotionService.updatePromotion(1L, testPromotionDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(promotionRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("deletePromotion should delete existing promotion")
    void whenDeletePromotion_thenDeletes() {
        // Arrange
        when(promotionRepository.existsById(1L)).thenReturn(true);

        // Act
        promotionService.deletePromotion(1L);

        // Assert
        verify(promotionRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("registerLead should create new lead")
    void whenRegisterLead_thenCreatesLead() {
        // Arrange
        PromotionLeadDTO leadDTO = new PromotionLeadDTO(
                null, 1L, "Jane", "Doe", "new@example.com", "1234567890",
                null, null, null, null);
        
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(objectMapper.convertValue(any(PromotionLeadDTO.class), eq(PromotionLead.class))).thenReturn(testLead);
        when(promotionLeadRepository.existsByPromotionIdAndIdentifier(anyLong(), anyString())).thenReturn(false);
        when(promotionLeadRepository.save(any(PromotionLead.class))).thenReturn(testLead);
        when(objectMapper.convertValue(any(PromotionLead.class), eq(PromotionLeadDTO.class))).thenReturn(leadDTO);

        // Act
        PromotionLeadDTO result = promotionService.registerLead(1L, leadDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(promotionLeadRepository, times(1)).save(any(PromotionLead.class));
    }

    @Test
    @DisplayName("registerLead should throw exception for duplicate identifier")
    void whenRegisterLeadWithDuplicateIdentifier_thenThrowsException() {
        // Arrange
        PromotionLeadDTO leadDTO = new PromotionLeadDTO(
                null, 1L, "Jane", "Doe", "test@example.com", null,
                null, null, null, null);
        
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(objectMapper.convertValue(any(PromotionLeadDTO.class), eq(PromotionLead.class))).thenReturn(testLead);
        when(promotionLeadRepository.existsByPromotionIdAndIdentifier(1L, "test@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> promotionService.registerLead(1L, leadDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("listLeads should return all leads for promotion")
    void whenListLeads_thenReturnsAllLeads() {
        // Arrange
        PromotionLeadDTO leadDTO = new PromotionLeadDTO(
                1L, 1L, "John", "Doe", "test@example.com", null,
                null, null, null, null);
        
        when(promotionLeadRepository.findByPromotionId(1L)).thenReturn(List.of(testLead));
        when(objectMapper.convertValue(any(PromotionLead.class), eq(PromotionLeadDTO.class))).thenReturn(leadDTO);

        // Act
        List<PromotionLeadDTO> result = promotionService.listLeads(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("createTestLead should create test lead with defaults")
    void whenCreateTestLead_thenCreatesTestLead() {
        // Arrange
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(testPromotion));
        when(promotionLeadRepository.save(any(PromotionLead.class))).thenReturn(testLead);
        
        PromotionLeadDTO testDTO = new PromotionLeadDTO(
                1L, 1L, "Test", "Lead", "test@example.test", null,
                null, null, null, null);
        when(objectMapper.convertValue(any(PromotionLead.class), eq(PromotionLeadDTO.class))).thenReturn(testDTO);

        // Act
        PromotionLeadDTO result = promotionService.createTestLead(1L, null);

        // Assert
        assertThat(result).isNotNull();
        verify(promotionLeadRepository, times(1)).save(any(PromotionLead.class));
    }
}
