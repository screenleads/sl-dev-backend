package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.AudienceSegment;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.UserSegment;
import com.screenleads.backend.app.domain.repository.AudienceSegmentRepository;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;
import com.screenleads.backend.app.service.AudienceSegmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudienceSegmentServiceImplTest {

    @Mock
    private AudienceSegmentRepository audienceSegmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AudienceSegmentServiceImpl audienceSegmentService;

    private Company company;
    private AudienceSegment segment;
    private Customer customer;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        segment = new AudienceSegment();
        segment.setId(1L);
        segment.setName("Test Segment");
        segment.setDescription("Test Description");
        segment.setCompany(company);
        segment.setIsActive(true);
        segment.setFilterRules(new HashMap<>());

        customer = new Customer();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        customer.setTotalRedemptions(5);
        customer.setLifetimeValue(BigDecimal.valueOf(100.00));
        customer.setEngagementScore(75);
        customer.setSegment(UserSegment.WARM);
        customer.setEmailVerified(true);
        customer.setMarketingOptIn(true);
        customer.setLastInteractionAt(Instant.now());
    }

    @Test
    void testCreateSegment_Success() {
        // Arrange
        when(audienceSegmentRepository.save(any(AudienceSegment.class))).thenReturn(segment);
        when(audienceSegmentRepository.findById(1L)).thenReturn(Optional.of(segment));
        when(customerRepository.findByCompanyId(anyLong())).thenReturn(List.of(customer));

        // Act
        AudienceSegment created = audienceSegmentService.createSegment(segment);

        // Assert
        assertNotNull(created);
        assertEquals("Test Segment", created.getName());
        verify(audienceSegmentRepository, times(2)).save(any(AudienceSegment.class)); // Once for create, once for count update
        verify(customerRepository).findByCompanyId(1L);
    }

    @Test
    void testUpdateSegment_Success() {
        // Arrange
        AudienceSegment updatedSegment = new AudienceSegment();
        updatedSegment.setName("Updated Segment");
        updatedSegment.setDescription("Updated Description");
        updatedSegment.setFilterRules(Map.of("minRedemptions", 10));

        when(audienceSegmentRepository.findById(1L)).thenReturn(Optional.of(segment));
        when(audienceSegmentRepository.save(any(AudienceSegment.class))).thenReturn(segment);
        when(customerRepository.findByCompanyId(anyLong())).thenReturn(List.of(customer));

        // Act
        AudienceSegment result = audienceSegmentService.updateSegment(1L, updatedSegment);

        // Assert
        assertNotNull(result);
        verify(audienceSegmentRepository, atLeast(2)).findById(1L); // update + count update calls
        verify(audienceSegmentRepository, times(2)).save(any(AudienceSegment.class)); // Once for update, once for count
    }

    @Test
    void testUpdateSegment_NotFound() {
        // Arrange
        when(audienceSegmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                audienceSegmentService.updateSegment(999L, new AudienceSegment())
        );
    }

    @Test
    void testDeleteSegment_Success() {
        // Act
        audienceSegmentService.deleteSegment(1L);

        // Assert
        verify(audienceSegmentRepository).deleteById(1L);
    }

    @Test
    void testGetSegmentById_Found() {
        // Arrange
        when(audienceSegmentRepository.findById(1L)).thenReturn(Optional.of(segment));

        // Act
        Optional<AudienceSegment> result = audienceSegmentService.getSegmentById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Segment", result.get().getName());
    }

    @Test
    void testGetSegmentById_NotFound() {
        // Arrange
        when(audienceSegmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<AudienceSegment> result = audienceSegmentService.getSegmentById(999L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testGetSegmentsByCompany() {
        // Arrange
        List<AudienceSegment> segments = List.of(segment);
        when(audienceSegmentRepository.findByCompany_IdOrderByCreatedAtDesc(1L)).thenReturn(segments);

        // Act
        List<AudienceSegment> result = audienceSegmentService.getSegmentsByCompany(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Segment", result.get(0).getName());
    }

    @Test
    void testGetActiveSegmentsByCompany() {
        // Arrange
        segment.setIsActive(true);
        when(audienceSegmentRepository.findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(segment));

        // Act
        List<AudienceSegment> result = audienceSegmentService.getActiveSegmentsByCompany(1L);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void testCustomerMatchesSegment_NoFilters_MatchesAll() {
        // Arrange
        segment.setFilterRules(new HashMap<>());

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_MinRedemptions_Matches() {
        // Arrange
        segment.setFilterRules(Map.of("minRedemptions", 3));
        customer.setTotalRedemptions(5);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_MinRedemptions_DoesNotMatch() {
        // Arrange
        segment.setFilterRules(Map.of("minRedemptions", 10));
        customer.setTotalRedemptions(5);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testCustomerMatchesSegment_MinLifetimeValue_Matches() {
        // Arrange
        segment.setFilterRules(Map.of("minLifetimeValue", 50.0));
        customer.setLifetimeValue(BigDecimal.valueOf(100.0));

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_MinLifetimeValue_DoesNotMatch() {
        // Arrange
        segment.setFilterRules(Map.of("minLifetimeValue", 200.0));
        customer.setLifetimeValue(BigDecimal.valueOf(100.0));

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testCustomerMatchesSegment_LastInteractionDays_RecentInteraction_Matches() {
        // Arrange
        segment.setFilterRules(Map.of("lastInteractionDays", 30));
        customer.setLastInteractionAt(Instant.now().minus(10, java.time.temporal.ChronoUnit.DAYS));

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_LastInteractionDays_OldInteraction_DoesNotMatch() {
        // Arrange
        segment.setFilterRules(Map.of("lastInteractionDays", 7));
        customer.setLastInteractionAt(Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS));

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testCustomerMatchesSegment_EmailVerified_Matches() {
        // Arrange
        segment.setFilterRules(Map.of("emailVerified", true));
        customer.setEmailVerified(true);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_EmailVerified_DoesNotMatch() {
        // Arrange
        segment.setFilterRules(Map.of("emailVerified", true));
        customer.setEmailVerified(false);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testCustomerMatchesSegment_MarketingOptIn_Matches() {
        // Arrange
        segment.setFilterRules(Map.of("marketingOptIn", true));
        customer.setMarketingOptIn(true);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_Segment_Matches() {
        // Arrange
        segment.setFilterRules(Map.of("segment", "WARM"));
        customer.setSegment(UserSegment.WARM);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_Segment_DoesNotMatch() {
        // Arrange
        segment.setFilterRules(Map.of("segment", "HOT"));
        customer.setSegment(UserSegment.WARM);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testCustomerMatchesSegment_EngagementScoreRange_Matches() {
        // Arrange
        segment.setFilterRules(Map.of(
                "minEngagementScore", 50,
                "maxEngagementScore", 100
        ));
        customer.setEngagementScore(75);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_EngagementScoreTooLow_DoesNotMatch() {
        // Arrange
        segment.setFilterRules(Map.of("minEngagementScore", 80));
        customer.setEngagementScore(50);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testCustomerMatchesSegment_AgeRange_Matches() {
        // Arrange
        segment.setFilterRules(Map.of(
                "minAge", 25,
                "maxAge", 40
        ));
        customer.setBirthDate(LocalDate.now().minusYears(30));

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_AgeTooYoung_DoesNotMatch() {
        // Arrange
        segment.setFilterRules(Map.of("minAge", 30));
        customer.setBirthDate(LocalDate.now().minusYears(20));

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testCustomerMatchesSegment_City_Matches() {
        // Arrange
        segment.setFilterRules(Map.of("city", "Barcelona"));
        customer.setCity("Barcelona");

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_City_DoesNotMatch() {
        // Arrange
        segment.setFilterRules(Map.of("city", "Madrid"));
        customer.setCity("Barcelona");

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testCustomerMatchesSegment_Country_Matches() {
        // Arrange
        segment.setFilterRules(Map.of("country", "Spain"));
        customer.setCountry("Spain");

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_MultipleFilters_AllMatch() {
        // Arrange
        segment.setFilterRules(Map.of(
                "minRedemptions", 3,
                "minLifetimeValue", 50.0,
                "emailVerified", true,
                "segment", "WARM"
        ));
        customer.setTotalRedemptions(5);
        customer.setLifetimeValue(BigDecimal.valueOf(100.0));
        customer.setEmailVerified(true);
        customer.setSegment(UserSegment.WARM);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertTrue(matches);
    }

    @Test
    void testCustomerMatchesSegment_MultipleFilters_OneFails_DoesNotMatch() {
        // Arrange
        segment.setFilterRules(Map.of(
                "minRedemptions", 3,
                "emailVerified", true,
                "segment", "HOT" // This won't match
        ));
        customer.setTotalRedemptions(5);
        customer.setEmailVerified(true);
        customer.setSegment(UserSegment.WARM);

        // Act
        boolean matches = audienceSegmentService.customerMatchesSegment(customer, segment);

        // Assert
        assertFalse(matches);
    }

    @Test
    void testGetCustomersInSegment_WithFilters() {
        // Arrange
        Customer customer1 = new Customer();
        customer1.setId(1L);
        customer1.setTotalRedemptions(10);

        Customer customer2 = new Customer();
        customer2.setId(2L);
        customer2.setTotalRedemptions(5);

        segment.setFilterRules(Map.of("minRedemptions", 8));

        when(audienceSegmentRepository.findById(1L)).thenReturn(Optional.of(segment));
        when(customerRepository.findByCompanyId(1L)).thenReturn(List.of(customer1, customer2));

        // Act
        List<Customer> result = audienceSegmentService.getCustomersInSegment(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getTotalRedemptions());
    }

    @Test
    void testUpdateSegmentCustomerCount() {
        // Arrange
        when(audienceSegmentRepository.findById(1L)).thenReturn(Optional.of(segment));
        when(customerRepository.findByCompanyId(1L)).thenReturn(List.of(customer));
        when(audienceSegmentRepository.save(any(AudienceSegment.class))).thenReturn(segment);

        // Act
        audienceSegmentService.updateSegmentCustomerCount(1L);

        // Assert
        verify(audienceSegmentRepository).save(any(AudienceSegment.class));
        verify(customerRepository).findByCompanyId(1L);
    }

    @Test
    void testRecalculateAllSegmentCounts() {
        // Arrange
        AudienceSegment segment1 = new AudienceSegment();
        segment1.setId(1L);
        segment1.setCompany(company);

        AudienceSegment segment2 = new AudienceSegment();
        segment2.setId(2L);
        segment2.setCompany(company);

        when(audienceSegmentRepository.findSegmentsNeedingRecalculation())
                .thenReturn(List.of(segment1, segment2));
        when(audienceSegmentRepository.findById(anyLong())).thenReturn(Optional.of(segment1));
        when(customerRepository.findByCompanyId(anyLong())).thenReturn(List.of(customer));
        when(audienceSegmentRepository.save(any(AudienceSegment.class))).thenReturn(segment1);

        // Act
        audienceSegmentService.recalculateAllSegmentCounts();

        // Assert
        verify(audienceSegmentRepository).findSegmentsNeedingRecalculation();
        verify(audienceSegmentRepository, atLeast(2)).findById(anyLong());
    }

    @Test
    void testPreviewSegmentMatch_LimitsTo100() {
        // Arrange
        List<Customer> manyCustomers = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            Customer c = new Customer();
            c.setId((long) i);
            manyCustomers.add(c);
        }

        when(customerRepository.findByCompanyId(1L)).thenReturn(manyCustomers);

        // Act
        List<Customer> result = audienceSegmentService.previewSegmentMatch(1L, segment);

        // Assert
        assertTrue(result.size() <= 100, "Preview should limit to 100 customers");
    }

    @Test
    void testToggleSegmentActive() {
        // Arrange
        when(audienceSegmentRepository.findById(1L)).thenReturn(Optional.of(segment));
        when(audienceSegmentRepository.save(any(AudienceSegment.class))).thenReturn(segment);

        // Act
        AudienceSegment result = audienceSegmentService.toggleSegmentActive(1L, false);

        // Assert
        verify(audienceSegmentRepository).save(any(AudienceSegment.class));
    }

    @Test
    void testIsSegmentNameUnique_NewName_ReturnsTrue() {
        // Arrange
        when(audienceSegmentRepository.findByNameAndCompany_Id("New Segment", 1L))
                .thenReturn(Optional.empty());

        // Act
        boolean isUnique = audienceSegmentService.isSegmentNameUnique("New Segment", 1L, null);

        // Assert
        assertTrue(isUnique);
    }

    @Test
    void testIsSegmentNameUnique_ExistingName_SameId_ReturnsTrue() {
        // Arrange
        segment.setId(1L);
        when(audienceSegmentRepository.findByNameAndCompany_Id("Test Segment", 1L))
                .thenReturn(Optional.of(segment));

        // Act
        boolean isUnique = audienceSegmentService.isSegmentNameUnique("Test Segment", 1L, 1L);

        // Assert
        assertTrue(isUnique);
    }

    @Test
    void testIsSegmentNameUnique_ExistingName_DifferentId_ReturnsFalse() {
        // Arrange
        segment.setId(1L);
        when(audienceSegmentRepository.findByNameAndCompany_Id("Test Segment", 1L))
                .thenReturn(Optional.of(segment));

        // Act
        boolean isUnique = audienceSegmentService.isSegmentNameUnique("Test Segment", 1L, 999L);

        // Assert
        assertFalse(isUnique);
    }

    @Test
    void testCountCustomersInSegment() {
        // Arrange
        when(audienceSegmentRepository.findById(1L)).thenReturn(Optional.of(segment));
        when(customerRepository.findByCompanyId(1L)).thenReturn(List.of(customer));

        // Act
        Long count = audienceSegmentService.countCustomersInSegment(1L);

        // Assert
        assertEquals(1L, count);
    }

    @Test
    void testRebuildSegment() {
        // Arrange
        when(audienceSegmentRepository.findById(1L)).thenReturn(Optional.of(segment));
        when(customerRepository.findByCompanyId(1L)).thenReturn(List.of(customer));
        when(audienceSegmentRepository.save(any(AudienceSegment.class))).thenReturn(segment);

        // Act
        audienceSegmentService.rebuildSegment(1L);

        // Assert
        verify(audienceSegmentRepository, atLeast(1)).findById(1L); // rebuild calls updateSegmentCustomerCount which calls getCustomersInSegment
        verify(audienceSegmentRepository, times(1)).save(any(AudienceSegment.class));
        verify(customerRepository).findByCompanyId(1L);
    }

    @Test
    void testRebuildSegment_NotFound_ThrowsException() {
        // Arrange
        when(audienceSegmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                audienceSegmentService.rebuildSegment(999L)
        );
    }
}
