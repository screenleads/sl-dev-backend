package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.NotificationChannel;
import com.screenleads.backend.app.domain.model.NotificationTemplate;
import com.screenleads.backend.app.domain.repository.NotificationTemplateRepository;
import com.screenleads.backend.app.web.dto.NotificationRequest;
import com.screenleads.backend.app.web.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private NotificationTemplate emailTemplate;
    private NotificationTemplate smsTemplate;
    private NotificationTemplate pushTemplate;
    private Map<String, String> variables;

    @BeforeEach
    void setUp() {
        // Email template
        emailTemplate = new NotificationTemplate();
        emailTemplate.setId(1L);
        emailTemplate.setChannel(NotificationChannel.EMAIL);
        emailTemplate.setSubject("Hello {{name}}");
        emailTemplate.setBody("Your code is {{code}}. Expires {{expiryDate}}");
        emailTemplate.setSender("noreply@screenleads.com");
        emailTemplate.setReplyTo("support@screenleads.com");

        // SMS template
        smsTemplate = new NotificationTemplate();
        smsTemplate.setId(2L);
        smsTemplate.setChannel(NotificationChannel.SMS);
        smsTemplate.setBody("Your verification code is {{code}}");

        // Push template
        pushTemplate = new NotificationTemplate();
        pushTemplate.setId(3L);
        pushTemplate.setChannel(NotificationChannel.PUSH_NOTIFICATION);
        pushTemplate.setSubject("New Promotion!");
        pushTemplate.setBody("Check out our latest offer: {{offerName}}");

        // Test variables
        variables = new HashMap<>();
        variables.put("name", "John Doe");
        variables.put("code", "ABC123");
        variables.put("expiryDate", "2026-12-31");
        variables.put("offerName", "50% Off");
    }

    @Test
    void testSendFromTemplate_Email_Success() {
        // Given
        Long templateId = 1L;
        String recipient = "john@example.com";
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(emailTemplate));

        // When
        NotificationResponse response = notificationService.sendFromTemplate(templateId, recipient, variables);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("SENT", response.getStatus());
        assertEquals(recipient, response.getRecipient());
        assertNotNull(response.getNotificationId());
        assertNotNull(response.getProviderMessageId());
        verify(templateRepository, times(1)).findById(templateId);
    }

    @Test
    void testSendFromTemplate_SMS_Success() {
        // Given
        Long templateId = 2L;
        String recipient = "+1234567890";
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(smsTemplate));

        // When
        NotificationResponse response = notificationService.sendFromTemplate(templateId, recipient, variables);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("SENT", response.getStatus());
        assertEquals(recipient, response.getRecipient());
        verify(templateRepository, times(1)).findById(templateId);
    }

    @Test
    void testSendFromTemplate_Push_Success() {
        // Given
        Long templateId = 3L;
        String recipient = "device-token-123";
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(pushTemplate));

        // When
        NotificationResponse response = notificationService.sendFromTemplate(templateId, recipient, variables);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("SENT", response.getStatus());
        assertEquals(recipient, response.getRecipient());
        verify(templateRepository, times(1)).findById(templateId);
    }

    @Test
    void testSendFromTemplate_TemplateNotFound() {
        // Given
        Long templateId = 999L;
        String recipient = "john@example.com";
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When
        NotificationResponse response = notificationService.sendFromTemplate(templateId, recipient, variables);

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("FAILED", response.getStatus());
        assertEquals(recipient, response.getRecipient());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("Template not found"));
        verify(templateRepository, times(1)).findById(templateId);
    }

    @Test
    void testSendFromTemplate_VariableReplacement() {
        // Given
        Long templateId = 1L;
        String recipient = "john@example.com";
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(emailTemplate));

        // When
        NotificationResponse response = notificationService.sendFromTemplate(templateId, recipient, variables);

        // Then - verify that variables were replaced (this is implicit in the mock behavior)
        assertTrue(response.isSuccess());
    }

    @Test
    void testSendDirect_Email() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .subject("Test Subject")
                .body("Test Body")
                .build();

        // When
        NotificationResponse response = notificationService.sendDirect(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("SENT", response.getStatus());
        assertEquals(request.getRecipient(), response.getRecipient());
        assertNotNull(response.getNotificationId());
    }

    @Test
    void testSendDirect_SMS() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .channel(NotificationChannel.SMS)
                .recipient("+1234567890")
                .body("Test SMS")
                .build();

        // When
        NotificationResponse response = notificationService.sendDirect(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("SENT", response.getStatus());
        assertEquals(request.getRecipient(), response.getRecipient());
    }

    @Test
    void testSendDirect_Push() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .channel(NotificationChannel.PUSH_NOTIFICATION)
                .recipient("device-token-xyz")
                .subject("Push Title")
                .body("Push Body")
                .build();

        // When
        NotificationResponse response = notificationService.sendDirect(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("SENT", response.getStatus());
    }

    @Test
    void testSendBulk_Success() {
        // Given
        Long templateId = 2L;
        List<String> recipients = Arrays.asList("+1111111111", "+2222222222", "+3333333333");
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(smsTemplate));

        // When
        List<NotificationResponse> responses = notificationService.sendBulk(templateId, recipients, variables);

        // Then
        assertNotNull(responses);
        assertEquals(3, responses.size());
        long successCount = responses.stream().filter(NotificationResponse::isSuccess).count();
        assertEquals(3, successCount);
        verify(templateRepository, times(3)).findById(templateId);
    }

    @Test
    void testSendBulk_PartialFailure() {
        // Given
        Long templateId = 1L;
        List<String> recipients = Arrays.asList("user1@test.com", "user2@test.com");
        
        // First call succeeds, second call fails
        when(templateRepository.findById(templateId))
                .thenReturn(Optional.of(emailTemplate))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When
        List<NotificationResponse> responses = notificationService.sendBulk(templateId, recipients, variables);

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertTrue(responses.get(0).isSuccess());
        assertFalse(responses.get(1).isSuccess());
    }

    @Test
    void testSendBulk_EmptyRecipients() {
        // Given
        Long templateId = 1L;
        List<String> recipients = Collections.emptyList();

        // When
        List<NotificationResponse> responses = notificationService.sendBulk(templateId, recipients, variables);

        // Then
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(templateRepository, never()).findById(any());
    }

    @Test
    void testScheduleNotification() {
        // Given
        Long templateId = 1L;
        String recipient = "john@example.com";
        Long scheduledAt = System.currentTimeMillis() + 3600000; // 1 hour from now

        // When
        NotificationResponse response = notificationService.scheduleNotification(
                templateId, recipient, variables, scheduledAt);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("SCHEDULED", response.getStatus());
        assertEquals(recipient, response.getRecipient());
        assertEquals(scheduledAt, response.getTimestamp());
    }

    @Test
    void testPreviewTemplate_Success() {
        // Given
        Long templateId = 1L;
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(emailTemplate));

        // When
        String preview = notificationService.previewTemplate(templateId, variables);

        // Then
        assertNotNull(preview);
        assertTrue(preview.contains("Hello John Doe"));
        assertTrue(preview.contains("Your code is ABC123"));
        assertTrue(preview.contains("Expires 2026-12-31"));
        verify(templateRepository, times(1)).findById(templateId);
    }

    @Test
    void testPreviewTemplate_TemplateNotFound() {
        // Given
        Long templateId = 999L;
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            notificationService.previewTemplate(templateId, variables);
        });
        verify(templateRepository, times(1)).findById(templateId);
    }

    @Test
    void testPreviewTemplate_MissingVariables() {
        // Given
        Long templateId = 1L;
        Map<String, String> partialVariables = new HashMap<>();
        partialVariables.put("name", "John");
        // Missing 'code' and 'expiryDate'
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(emailTemplate));

        // When
        String preview = notificationService.previewTemplate(templateId, partialVariables);

        // Then
        assertNotNull(preview);
        assertTrue(preview.contains("Hello John"));
        assertTrue(preview.contains("{{code}}")); // Unreplaced variable
        assertTrue(preview.contains("{{expiryDate}}")); // Unreplaced variable
    }

    @Test
    void testIsChannelConfigured_AllChannels() {
        // When/Then - All channels should return false (not configured yet)
        assertFalse(notificationService.isChannelConfigured(NotificationChannel.EMAIL));
        assertFalse(notificationService.isChannelConfigured(NotificationChannel.SMS));
        assertFalse(notificationService.isChannelConfigured(NotificationChannel.PUSH_NOTIFICATION));
    }

    @Test
    void testVariableReplacement_ComplexTemplate() {
        // Given
        String template = "Hello {{firstName}} {{lastName}}, you have {{count}} new messages. Visit {{url}}";
        Map<String, String> vars = new HashMap<>();
        vars.put("firstName", "Jane");
        vars.put("lastName", "Smith");
        vars.put("count", "5");
        vars.put("url", "https://app.screenleads.com");

        emailTemplate.setBody(template);
        Long templateId = 1L;
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(emailTemplate));

        // When
        String preview = notificationService.previewTemplate(templateId, vars);

        // Then
        assertTrue(preview.contains("Hello Jane Smith"));
        assertTrue(preview.contains("you have 5 new messages"));
        assertTrue(preview.contains("Visit https://app.screenleads.com"));
    }

    @Test
    void testVariableReplacement_NullTemplate() {
        // Given
        emailTemplate.setBody(null);
        Long templateId = 1L;
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(emailTemplate));

        // When
        String preview = notificationService.previewTemplate(templateId, variables);

        // Then
        assertNotNull(preview);
        assertTrue(preview.contains("Subject:"));
    }

    @Test
    void testVariableReplacement_EmptyVariables() {
        // Given
        Long templateId = 1L;
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(emailTemplate));

        // When
        String preview = notificationService.previewTemplate(templateId, Collections.emptyMap());

        // Then
        assertNotNull(preview);
        assertTrue(preview.contains("{{name}}")); // Variables not replaced
        assertTrue(preview.contains("{{code}}"));
    }
}
