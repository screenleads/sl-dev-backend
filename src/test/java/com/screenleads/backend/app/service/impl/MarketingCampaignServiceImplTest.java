package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repository.MarketingCampaignRepository;
import com.screenleads.backend.app.service.AudienceSegmentService;
import com.screenleads.backend.app.service.NotificationService;
import com.screenleads.backend.app.service.NotificationTemplateService;
import com.screenleads.backend.app.web.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketingCampaignServiceImplTest {

    @Mock
    private MarketingCampaignRepository campaignRepository;

    @Mock
    private AudienceSegmentService audienceSegmentService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MarketingCampaignServiceImpl campaignService;

    private Company company;
    private AudienceSegment segment;
    private NotificationTemplate template;
    private MarketingCampaign campaign;
    private Customer customer;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        segment = new AudienceSegment();
        segment.setId(1L);
        segment.setName("Test Segment");
        segment.setCompany(company);

        template = new NotificationTemplate();
        template.setId(1L);
        template.setName("Test Template");
        template.setChannel(NotificationChannel.EMAIL);
        template.setSubject("Test Subject");
        template.setBody("Hello {{customerName}}");

        campaign = new MarketingCampaign();
        campaign.setId(1L);
        campaign.setName("Test Campaign");
        campaign.setDescription("Test Description");
        campaign.setCompany(company);
        campaign.setAudienceSegment(segment);
        campaign.setNotificationTemplate(template);
        campaign.setStatus(CampaignStatus.DRAFT);

        customer = new Customer();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        customer.setPhone("+34600000000");
        customer.setFirstName("John");
        customer.setLastName("Doe");
    }

    @Test
    void testCreateCampaign_Success() {
        // Arrange
        when(audienceSegmentService.countCustomersInSegment(1L)).thenReturn(100L);
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        MarketingCampaign created = campaignService.createCampaign(campaign);

        // Assert
        assertNotNull(created);
        assertEquals("Test Campaign", created.getName());
        assertEquals(CampaignStatus.DRAFT, created.getStatus());
        verify(audienceSegmentService).countCustomersInSegment(1L);
        verify(campaignRepository).save(any(MarketingCampaign.class));
    }

    @Test
    void testUpdateCampaign_Success() {
        // Arrange
        MarketingCampaign updatedData = new MarketingCampaign();
        updatedData.setName("Updated Campaign");
        updatedData.setDescription("Updated Description");
        updatedData.setAudienceSegment(segment);
        updatedData.setNotificationTemplate(template);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        MarketingCampaign result = campaignService.updateCampaign(1L, updatedData);

        // Assert
        assertNotNull(result);
        verify(campaignRepository).findById(1L);
        verify(campaignRepository).save(any(MarketingCampaign.class));
    }

    @Test
    void testUpdateCampaign_NotFound() {
        // Arrange
        when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> campaignService.updateCampaign(999L, new MarketingCampaign()));
    }

    @Test
    void testUpdateCampaign_FinalizedCampaign_ThrowsException() {
        // Arrange
        campaign.setStatus(CampaignStatus.COMPLETED);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> campaignService.updateCampaign(1L, new MarketingCampaign()));
    }

    @Test
    void testUpdateCampaign_ChangesSegment_RecalculatesAudience() {
        // Arrange
        AudienceSegment newSegment = new AudienceSegment();
        newSegment.setId(2L);
        newSegment.setName("New Segment");

        MarketingCampaign updatedData = new MarketingCampaign();
        updatedData.setName("Updated Campaign");
        updatedData.setAudienceSegment(newSegment);
        updatedData.setNotificationTemplate(template);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(audienceSegmentService.countCustomersInSegment(2L)).thenReturn(200L);
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        campaignService.updateCampaign(1L, updatedData);

        // Assert
        verify(audienceSegmentService).countCustomersInSegment(2L);
    }

    @Test
    void testDeleteCampaign_DraftStatus_Success() {
        // Arrange
        campaign.setStatus(CampaignStatus.DRAFT);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act
        campaignService.deleteCampaign(1L);

        // Assert
        verify(campaignRepository).delete(campaign);
    }

    @Test
    void testDeleteCampaign_NonDraftStatus_ThrowsException() {
        // Arrange
        campaign.setStatus(CampaignStatus.RUNNING);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> campaignService.deleteCampaign(1L));
        verify(campaignRepository, never()).delete(any());
    }

    @Test
    void testGetCampaignById_Found() {
        // Arrange
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act
        Optional<MarketingCampaign> result = campaignService.getCampaignById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Campaign", result.get().getName());
    }

    @Test
    void testGetCampaignById_NotFound() {
        // Arrange
        when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<MarketingCampaign> result = campaignService.getCampaignById(999L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testGetCampaignsByCompany() {
        // Arrange
        List<MarketingCampaign> campaigns = List.of(campaign);
        when(campaignRepository.findByCompany_IdOrderByCreatedAtDesc(1L)).thenReturn(campaigns);

        // Act
        List<MarketingCampaign> result = campaignService.getCampaignsByCompany(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Campaign", result.get(0).getName());
    }

    @Test
    void testGetCampaignsByStatus() {
        // Arrange
        campaign.setStatus(CampaignStatus.COMPLETED);
        when(campaignRepository.findByCompany_IdAndStatusOrderByCreatedAtDesc(1L, CampaignStatus.COMPLETED))
                .thenReturn(List.of(campaign));

        // Act
        List<MarketingCampaign> result = campaignService.getCampaignsByStatus(1L, CampaignStatus.COMPLETED);

        // Assert
        assertEquals(1, result.size());
        assertEquals(CampaignStatus.COMPLETED, result.get(0).getStatus());
    }

    @Test
    void testScheduleCampaign_Success() {
        // Arrange
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        campaign.setStatus(CampaignStatus.DRAFT);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        MarketingCampaign result = campaignService.scheduleCampaign(1L, futureDate);

        // Assert
        assertNotNull(result);
        verify(campaignRepository).save(any(MarketingCampaign.class));
    }

    @Test
    void testScheduleCampaign_NonDraftStatus_ThrowsException() {
        // Arrange
        campaign.setStatus(CampaignStatus.RUNNING);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> campaignService.scheduleCampaign(1L, LocalDateTime.now().plusDays(1)));
    }

    @Test
    void testScheduleCampaign_PastDate_ThrowsException() {
        // Arrange
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        campaign.setStatus(CampaignStatus.DRAFT);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> campaignService.scheduleCampaign(1L, pastDate));
    }

    @Test
    void testExecuteCampaign_Success() {
        // Arrange
        campaign.setStatus(CampaignStatus.DRAFT);
        List<Customer> customers = List.of(customer);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(audienceSegmentService.getCustomersInSegment(1L)).thenReturn(customers);
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        NotificationResponse successResponse = NotificationResponse.builder()
                .success(true)
                .notificationId("notif-123")
                .build();
        when(notificationService.sendFromTemplate(anyLong(), anyString(), anyMap()))
                .thenReturn(successResponse);

        // Act
        MarketingCampaign result = campaignService.executeCampaign(1L);

        // Assert
        assertNotNull(result);
        verify(audienceSegmentService).getCustomersInSegment(1L);
        verify(notificationService, atLeastOnce()).sendFromTemplate(anyLong(), anyString(), anyMap());
        verify(notificationTemplateService).incrementUsageCount(1L);
    }

    @Test
    void testExecuteCampaign_FinalizedCampaign_ThrowsException() {
        // Arrange
        campaign.setStatus(CampaignStatus.COMPLETED);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> campaignService.executeCampaign(1L));
    }

    @Test
    void testExecuteCampaign_EmailChannel_Success() {
        // Arrange
        template.setChannel(NotificationChannel.EMAIL);
        campaign.setStatus(CampaignStatus.DRAFT);
        List<Customer> customers = List.of(customer);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(audienceSegmentService.getCustomersInSegment(1L)).thenReturn(customers);
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        NotificationResponse successResponse = NotificationResponse.builder()
                .success(true)
                .notificationId("email-123")
                .build();
        when(notificationService.sendFromTemplate(eq(1L), eq("test@example.com"), anyMap()))
                .thenReturn(successResponse);

        // Act
        campaignService.executeCampaign(1L);

        // Assert
        verify(notificationService).sendFromTemplate(eq(1L), eq("test@example.com"), anyMap());
    }

    @Test
    void testExecuteCampaign_SMSChannel_Success() {
        // Arrange
        template.setChannel(NotificationChannel.SMS);
        campaign.setStatus(CampaignStatus.DRAFT);
        List<Customer> customers = List.of(customer);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(audienceSegmentService.getCustomersInSegment(1L)).thenReturn(customers);
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        NotificationResponse successResponse = NotificationResponse.builder()
                .success(true)
                .notificationId("sms-123")
                .build();
        when(notificationService.sendFromTemplate(eq(1L), eq("+34600000000"), anyMap()))
                .thenReturn(successResponse);

        // Act
        campaignService.executeCampaign(1L);

        // Assert
        verify(notificationService).sendFromTemplate(eq(1L), eq("+34600000000"), anyMap());
    }

    @Test
    void testExecuteCampaign_PushChannel_SkipsCustomer() {
        // Arrange
        template.setChannel(NotificationChannel.PUSH_NOTIFICATION);
        campaign.setStatus(CampaignStatus.DRAFT);
        List<Customer> customers = List.of(customer);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(audienceSegmentService.getCustomersInSegment(1L)).thenReturn(customers);
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        campaignService.executeCampaign(1L);

        // Assert
        verify(notificationService, never()).sendFromTemplate(anyLong(), anyString(), anyMap());
    }

    @Test
    void testExecuteCampaign_MixedSuccessAndFailure() {
        // Arrange
        campaign.setStatus(CampaignStatus.DRAFT);
        Customer customer2 = new Customer();
        customer2.setId(2L);
        customer2.setEmail("test2@example.com");
        customer2.setFirstName("Jane");
        customer2.setLastName("Smith");

        List<Customer> customers = List.of(customer, customer2);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(audienceSegmentService.getCustomersInSegment(1L)).thenReturn(customers);
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        NotificationResponse successResponse = NotificationResponse.builder()
                .success(true)
                .notificationId("notif-123")
                .build();

        NotificationResponse failureResponse = NotificationResponse.builder()
                .success(false)
                .errorMessage("Failed to send")
                .build();

        when(notificationService.sendFromTemplate(eq(1L), eq("test@example.com"), anyMap()))
                .thenReturn(successResponse);
        when(notificationService.sendFromTemplate(eq(1L), eq("test2@example.com"), anyMap()))
                .thenReturn(failureResponse);

        // Act
        campaignService.executeCampaign(1L);

        // Assert
        verify(notificationService, times(2)).sendFromTemplate(anyLong(), anyString(), anyMap());
    }

    @Test
    void testExecuteCampaign_CustomerWithoutEmail_Skipped() {
        // Arrange
        customer.setEmail(null);
        campaign.setStatus(CampaignStatus.DRAFT);
        List<Customer> customers = List.of(customer);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(audienceSegmentService.getCustomersInSegment(1L)).thenReturn(customers);
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        campaignService.executeCampaign(1L);

        // Assert
        verify(notificationService, never()).sendFromTemplate(anyLong(), anyString(), anyMap());
    }

    @Test
    void testExecuteCampaign_Exception_SetsFailed() {
        // Arrange
        campaign.setStatus(CampaignStatus.DRAFT);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(audienceSegmentService.getCustomersInSegment(1L))
                .thenThrow(new RuntimeException("Database error"));
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        campaignService.executeCampaign(1L);

        // Assert
        verify(campaignRepository, atLeast(2)).save(any(MarketingCampaign.class));
    }

    @Test
    void testPauseCampaign_RunningCampaign_Success() {
        // Arrange
        campaign.setStatus(CampaignStatus.RUNNING);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        MarketingCampaign result = campaignService.pauseCampaign(1L);

        // Assert
        assertNotNull(result);
        verify(campaignRepository).save(any(MarketingCampaign.class));
    }

    @Test
    void testPauseCampaign_NonRunningCampaign_ThrowsException() {
        // Arrange
        campaign.setStatus(CampaignStatus.DRAFT);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> campaignService.pauseCampaign(1L));
    }

    @Test
    void testResumeCampaign_PausedCampaign_Success() {
        // Arrange
        campaign.setStatus(CampaignStatus.PAUSED);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        MarketingCampaign result = campaignService.resumeCampaign(1L);

        // Assert
        assertNotNull(result);
        verify(campaignRepository).save(any(MarketingCampaign.class));
    }

    @Test
    void testResumeCampaign_NonPausedCampaign_ThrowsException() {
        // Arrange
        campaign.setStatus(CampaignStatus.DRAFT);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> campaignService.resumeCampaign(1L));
    }

    @Test
    void testCancelCampaign_ScheduledCampaign_Success() {
        // Arrange
        campaign.setStatus(CampaignStatus.SCHEDULED);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        MarketingCampaign result = campaignService.cancelCampaign(1L);

        // Assert
        assertNotNull(result);
        verify(campaignRepository).save(any(MarketingCampaign.class));
    }

    @Test
    void testCancelCampaign_RunningCampaign_Success() {
        // Arrange
        campaign.setStatus(CampaignStatus.RUNNING);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        // Act
        MarketingCampaign result = campaignService.cancelCampaign(1L);

        // Assert
        assertNotNull(result);
        verify(campaignRepository).save(any(MarketingCampaign.class));
    }

    @Test
    void testCancelCampaign_FinalizedCampaign_ThrowsException() {
        // Arrange
        campaign.setStatus(CampaignStatus.COMPLETED);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> campaignService.cancelCampaign(1L));
    }

    @Test
    void testGetCampaignStatistics() {
        // Arrange
        campaign.setSuccessCount(100L);
        campaign.setFailedCount(5L);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

        // Act
        MarketingCampaign result = campaignService.getCampaignStatistics(1L);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getSuccessCount());
        assertEquals(5L, result.getFailedCount());
    }

    @Test
    void testProcessPendingCampaigns() {
        // Arrange
        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign.setScheduledAt(LocalDateTime.now().minusMinutes(5));
        List<MarketingCampaign> pendingCampaigns = List.of(campaign);

        when(campaignRepository.findPendingCampaigns(any(LocalDateTime.class)))
                .thenReturn(pendingCampaigns);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(audienceSegmentService.getCustomersInSegment(1L)).thenReturn(List.of(customer));
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        NotificationResponse successResponse = NotificationResponse.builder()
                .success(true)
                .notificationId("notif-123")
                .build();
        when(notificationService.sendFromTemplate(anyLong(), anyString(), anyMap()))
                .thenReturn(successResponse);

        // Act
        campaignService.processPendingCampaigns();

        // Assert
        verify(campaignRepository).findPendingCampaigns(any(LocalDateTime.class));
        verify(audienceSegmentService).getCustomersInSegment(1L);
    }

    @Test
    void testProcessPendingCampaigns_WithException_ContinuesProcessing() {
        // Arrange
        MarketingCampaign campaign2 = new MarketingCampaign();
        campaign2.setId(2L);
        campaign2.setName("Campaign 2");
        campaign2.setCompany(company);
        campaign2.setAudienceSegment(segment);
        campaign2.setNotificationTemplate(template);
        campaign2.setStatus(CampaignStatus.SCHEDULED);

        List<MarketingCampaign> pendingCampaigns = List.of(campaign, campaign2);

        when(campaignRepository.findPendingCampaigns(any(LocalDateTime.class)))
                .thenReturn(pendingCampaigns);
        when(campaignRepository.findById(1L))
                .thenThrow(new RuntimeException("Database error"));
        when(campaignRepository.findById(2L)).thenReturn(Optional.of(campaign2));
        when(audienceSegmentService.getCustomersInSegment(1L)).thenReturn(List.of(customer));
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign2);

        NotificationResponse successResponse = NotificationResponse.builder()
                .success(true)
                .notificationId("notif-123")
                .build();
        when(notificationService.sendFromTemplate(anyLong(), anyString(), anyMap()))
                .thenReturn(successResponse);

        // Act
        campaignService.processPendingCampaigns();

        // Assert - Should process campaign2 even after campaign1 fails
        verify(campaignRepository).findPendingCampaigns(any(LocalDateTime.class));
    }

    @Test
    void testGetTopCampaigns() {
        // Arrange
        List<MarketingCampaign> topCampaigns = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MarketingCampaign c = new MarketingCampaign();
            c.setId((long) i);
            c.setName("Campaign " + i);
            topCampaigns.add(c);
        }

        when(campaignRepository.findTopCampaignsByOpenRate(1L)).thenReturn(topCampaigns);

        // Act
        List<MarketingCampaign> result = campaignService.getTopCampaigns(1L, 3);

        // Assert
        assertEquals(3, result.size());
        verify(campaignRepository).findTopCampaignsByOpenRate(1L);
    }

    @Test
    void testExecuteCampaign_VariablesPassedCorrectly() {
        // Arrange
        campaign.setStatus(CampaignStatus.DRAFT);
        List<Customer> customers = List.of(customer);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(audienceSegmentService.getCustomersInSegment(1L)).thenReturn(customers);
        when(campaignRepository.save(any(MarketingCampaign.class))).thenReturn(campaign);

        NotificationResponse successResponse = NotificationResponse.builder()
                .success(true)
                .notificationId("notif-123")
                .build();
        when(notificationService.sendFromTemplate(anyLong(), anyString(), anyMap()))
                .thenReturn(successResponse);

        // Act
        campaignService.executeCampaign(1L);

        // Assert
        verify(notificationService).sendFromTemplate(eq(1L), eq("test@example.com"), argThat(variables -> {
            Map<String, String> vars = (Map<String, String>) variables;
            return vars.containsKey("customerName") &&
                    vars.containsKey("email") &&
                    vars.containsKey("companyName") &&
                    vars.containsKey("campaignName") &&
                    vars.get("email").equals("test@example.com");
        }));
    }
}
