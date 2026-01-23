package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.NotificationChannel;
import com.screenleads.backend.app.domain.model.NotificationTemplate;
import com.screenleads.backend.app.domain.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceImplTest {

    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;

    @InjectMocks
    private NotificationTemplateServiceImpl notificationTemplateService;

    private Company company;
    private NotificationTemplate emailTemplate;
    private NotificationTemplate smsTemplate;
    private NotificationTemplate pushTemplate;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        emailTemplate = NotificationTemplate.builder()
                .id(1L)
                .name("Welcome Email")
                .description("Welcome email for new customers")
                .company(company)
                .channel(NotificationChannel.EMAIL)
                .subject("Welcome {{customerName}}!")
                .body("Hi {{customerName}}, welcome to {{companyName}}. Your code: {{code}}")
                .htmlBody("<h1>Hi {{customerName}}</h1><p>Welcome to {{companyName}}</p>")
                .availableVariables(Arrays.asList("customerName", "companyName", "code"))
                .isActive(true)
                .usageCount(10L)
                .sender("noreply@example.com")
                .replyTo("support@example.com")
                .build();

        smsTemplate = NotificationTemplate.builder()
                .id(2L)
                .name("SMS Promo")
                .description("SMS promotion template")
                .company(company)
                .channel(NotificationChannel.SMS)
                .body("Hi {{name}}, use code {{code}} for 20% off!")
                .availableVariables(Arrays.asList("name", "code"))
                .isActive(true)
                .usageCount(5L)
                .build();

        pushTemplate = NotificationTemplate.builder()
                .id(3L)
                .name("Push Notification")
                .description("Push notification template")
                .company(company)
                .channel(NotificationChannel.PUSH_NOTIFICATION)
                .subject("New offer!")
                .body("Check out {{offerName}}")
                .availableVariables(Arrays.asList("offerName"))
                .isActive(false)
                .usageCount(0L)
                .build();
    }

    @Test
    void createTemplate_Success() {
        // Arrange
        when(notificationTemplateRepository.save(any(NotificationTemplate.class)))
                .thenReturn(emailTemplate);

        // Act
        NotificationTemplate result = notificationTemplateService.createTemplate(emailTemplate);

        // Assert
        assertNotNull(result);
        assertEquals(emailTemplate.getName(), result.getName());
        assertEquals(NotificationChannel.EMAIL, result.getChannel());
        verify(notificationTemplateRepository).save(emailTemplate);
    }

    @Test
    void createTemplate_WithAllFields() {
        // Arrange
        NotificationTemplate fullTemplate = NotificationTemplate.builder()
                .name("Full Template")
                .description("Complete template with all fields")
                .company(company)
                .channel(NotificationChannel.EMAIL)
                .subject("Subject {{var1}}")
                .body("Body {{var2}}")
                .htmlBody("<p>HTML {{var3}}</p>")
                .availableVariables(Arrays.asList("var1", "var2", "var3"))
                .isActive(true)
                .sender("sender@test.com")
                .replyTo("reply@test.com")
                .metadata(Map.of("key", "value"))
                .build();

        when(notificationTemplateRepository.save(any(NotificationTemplate.class)))
                .thenReturn(fullTemplate);

        // Act
        NotificationTemplate result = notificationTemplateService.createTemplate(fullTemplate);

        // Assert
        assertNotNull(result);
        assertEquals("sender@test.com", result.getSender());
        assertEquals("reply@test.com", result.getReplyTo());
        assertNotNull(result.getMetadata());
        verify(notificationTemplateRepository).save(fullTemplate);
    }

    @Test
    void updateTemplate_Success() {
        // Arrange
        NotificationTemplate updatedData = NotificationTemplate.builder()
                .name("Updated Name")
                .description("Updated description")
                .channel(NotificationChannel.EMAIL)
                .subject("Updated subject {{name}}")
                .body("Updated body {{code}}")
                .htmlBody("<p>Updated HTML</p>")
                .availableVariables(Arrays.asList("name", "code"))
                .isActive(false)
                .sender("new@sender.com")
                .replyTo("new@reply.com")
                .metadata(Map.of("updated", "true"))
                .build();

        when(notificationTemplateRepository.findById(1L))
                .thenReturn(Optional.of(emailTemplate));
        when(notificationTemplateRepository.save(any(NotificationTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        NotificationTemplate result = notificationTemplateService.updateTemplate(1L, updatedData);

        // Assert
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertEquals("Updated subject {{name}}", result.getSubject());
        assertEquals("Updated body {{code}}", result.getBody());
        assertFalse(result.getIsActive());
        assertEquals("new@sender.com", result.getSender());
        verify(notificationTemplateRepository).findById(1L);
        verify(notificationTemplateRepository).save(emailTemplate);
    }

    @Test
    void updateTemplate_NotFound() {
        // Arrange
        when(notificationTemplateRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> notificationTemplateService.updateTemplate(999L, emailTemplate));
        verify(notificationTemplateRepository).findById(999L);
        verify(notificationTemplateRepository, never()).save(any());
    }

    @Test
    void deleteTemplate_Success() {
        // Arrange
        doNothing().when(notificationTemplateRepository).deleteById(1L);

        // Act
        notificationTemplateService.deleteTemplate(1L);

        // Assert
        verify(notificationTemplateRepository).deleteById(1L);
    }

    @Test
    void getTemplateById_Found() {
        // Arrange
        when(notificationTemplateRepository.findById(1L))
                .thenReturn(Optional.of(emailTemplate));

        // Act
        Optional<NotificationTemplate> result = notificationTemplateService.getTemplateById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(emailTemplate.getId(), result.get().getId());
        verify(notificationTemplateRepository).findById(1L);
    }

    @Test
    void getTemplateById_NotFound() {
        // Arrange
        when(notificationTemplateRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        Optional<NotificationTemplate> result = notificationTemplateService.getTemplateById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(notificationTemplateRepository).findById(999L);
    }

    @Test
    void getTemplatesByCompany_Success() {
        // Arrange
        List<NotificationTemplate> templates = Arrays.asList(emailTemplate, smsTemplate, pushTemplate);
        when(notificationTemplateRepository.findByCompany_IdOrderByCreatedAtDesc(1L))
                .thenReturn(templates);

        // Act
        List<NotificationTemplate> result = notificationTemplateService.getTemplatesByCompany(1L);

        // Assert
        assertEquals(3, result.size());
        verify(notificationTemplateRepository).findByCompany_IdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getTemplatesByCompany_EmptyList() {
        // Arrange
        when(notificationTemplateRepository.findByCompany_IdOrderByCreatedAtDesc(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<NotificationTemplate> result = notificationTemplateService.getTemplatesByCompany(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(notificationTemplateRepository).findByCompany_IdOrderByCreatedAtDesc(999L);
    }

    @Test
    void getActiveTemplatesByCompany_Success() {
        // Arrange
        List<NotificationTemplate> activeTemplates = Arrays.asList(emailTemplate, smsTemplate);
        when(notificationTemplateRepository.findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(1L))
                .thenReturn(activeTemplates);

        // Act
        List<NotificationTemplate> result = notificationTemplateService.getActiveTemplatesByCompany(1L);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(NotificationTemplate::getIsActive));
        verify(notificationTemplateRepository).findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(1L);
    }

    @Test
    void getActiveTemplatesByCompany_EmptyList() {
        // Arrange
        when(notificationTemplateRepository.findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<NotificationTemplate> result = notificationTemplateService.getActiveTemplatesByCompany(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(notificationTemplateRepository).findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(999L);
    }

    @Test
    void getTemplatesByCompanyAndChannel_Email() {
        // Arrange
        List<NotificationTemplate> emailTemplates = Arrays.asList(emailTemplate);
        when(notificationTemplateRepository.findByCompany_IdAndChannelAndIsActiveTrueOrderByCreatedAtDesc(
                1L, NotificationChannel.EMAIL))
                .thenReturn(emailTemplates);

        // Act
        List<NotificationTemplate> result = notificationTemplateService.getTemplatesByCompanyAndChannel(
                1L, NotificationChannel.EMAIL);

        // Assert
        assertEquals(1, result.size());
        assertEquals(NotificationChannel.EMAIL, result.get(0).getChannel());
        verify(notificationTemplateRepository).findByCompany_IdAndChannelAndIsActiveTrueOrderByCreatedAtDesc(
                1L, NotificationChannel.EMAIL);
    }

    @Test
    void getTemplatesByCompanyAndChannel_SMS() {
        // Arrange
        List<NotificationTemplate> smsTemplates = Arrays.asList(smsTemplate);
        when(notificationTemplateRepository.findByCompany_IdAndChannelAndIsActiveTrueOrderByCreatedAtDesc(
                1L, NotificationChannel.SMS))
                .thenReturn(smsTemplates);

        // Act
        List<NotificationTemplate> result = notificationTemplateService.getTemplatesByCompanyAndChannel(
                1L, NotificationChannel.SMS);

        // Assert
        assertEquals(1, result.size());
        assertEquals(NotificationChannel.SMS, result.get(0).getChannel());
        verify(notificationTemplateRepository).findByCompany_IdAndChannelAndIsActiveTrueOrderByCreatedAtDesc(
                1L, NotificationChannel.SMS);
    }

    @Test
    void getMostUsedTemplates_Success() {
        // Arrange
        List<NotificationTemplate> templates = Arrays.asList(emailTemplate, smsTemplate, pushTemplate);
        when(notificationTemplateRepository.findMostUsedByCompanyId(1L))
                .thenReturn(templates);

        // Act
        List<NotificationTemplate> result = notificationTemplateService.getMostUsedTemplates(1L, 2);

        // Assert
        assertEquals(2, result.size());
        verify(notificationTemplateRepository).findMostUsedByCompanyId(1L);
    }

    @Test
    void getMostUsedTemplates_AllResults() {
        // Arrange
        List<NotificationTemplate> templates = Arrays.asList(emailTemplate, smsTemplate);
        when(notificationTemplateRepository.findMostUsedByCompanyId(1L))
                .thenReturn(templates);

        // Act
        List<NotificationTemplate> result = notificationTemplateService.getMostUsedTemplates(1L, 10);

        // Assert
        assertEquals(2, result.size());
        verify(notificationTemplateRepository).findMostUsedByCompanyId(1L);
    }

    @Test
    void renderTemplateBody_Success() {
        // Arrange
        Map<String, String> variables = Map.of(
                "customerName", "John Doe",
                "companyName", "ACME Inc",
                "code", "WELCOME123");

        // Act
        String result = notificationTemplateService.renderTemplateBody(emailTemplate, variables);

        // Assert
        assertEquals("Hi John Doe, welcome to ACME Inc. Your code: WELCOME123", result);
    }

    @Test
    void renderTemplateBody_MissingVariables() {
        // Arrange
        Map<String, String> variables = Map.of("customerName", "John Doe");

        // Act
        String result = notificationTemplateService.renderTemplateBody(emailTemplate, variables);

        // Assert
        assertEquals("Hi John Doe, welcome to {{companyName}}. Your code: {{code}}", result);
    }

    @Test
    void renderTemplateBody_NullBody() {
        // Arrange
        NotificationTemplate templateWithNullBody = NotificationTemplate.builder()
                .id(10L)
                .name("Null Body Template")
                .body(null)
                .build();
        Map<String, String> variables = Map.of("var", "value");

        // Act
        String result = notificationTemplateService.renderTemplateBody(templateWithNullBody, variables);

        // Assert
        assertEquals("", result);
    }

    @Test
    void renderTemplateSubject_Success() {
        // Arrange
        Map<String, String> variables = Map.of("customerName", "Jane Smith");

        // Act
        String result = notificationTemplateService.renderTemplateSubject(emailTemplate, variables);

        // Assert
        assertEquals("Welcome Jane Smith!", result);
    }

    @Test
    void renderTemplateSubject_NullSubject() {
        // Arrange
        Map<String, String> variables = Map.of("var", "value");

        // Act
        String result = notificationTemplateService.renderTemplateSubject(smsTemplate, variables);

        // Assert
        assertEquals("", result);
    }

    @Test
    void incrementUsageCount_Success() {
        // Arrange
        when(notificationTemplateRepository.findById(1L))
                .thenReturn(Optional.of(emailTemplate));
        when(notificationTemplateRepository.save(any(NotificationTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Long originalCount = emailTemplate.getUsageCount();

        // Act
        notificationTemplateService.incrementUsageCount(1L);

        // Assert
        ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(notificationTemplateRepository).save(captor.capture());

        NotificationTemplate saved = captor.getValue();
        assertEquals(originalCount + 1, saved.getUsageCount());
        assertNotNull(saved.getLastUsedAt());
    }

    @Test
    void incrementUsageCount_NotFound() {
        // Arrange
        when(notificationTemplateRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> notificationTemplateService.incrementUsageCount(999L));
        verify(notificationTemplateRepository).findById(999L);
        verify(notificationTemplateRepository, never()).save(any());
    }

    @Test
    void toggleTemplateActive_ActivateInactive() {
        // Arrange
        when(notificationTemplateRepository.findById(3L))
                .thenReturn(Optional.of(pushTemplate));
        when(notificationTemplateRepository.save(any(NotificationTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertFalse(pushTemplate.getIsActive());

        // Act
        NotificationTemplate result = notificationTemplateService.toggleTemplateActive(3L, true);

        // Assert
        assertTrue(result.getIsActive());
        verify(notificationTemplateRepository).findById(3L);
        verify(notificationTemplateRepository).save(pushTemplate);
    }

    @Test
    void toggleTemplateActive_DeactivateActive() {
        // Arrange
        when(notificationTemplateRepository.findById(1L))
                .thenReturn(Optional.of(emailTemplate));
        when(notificationTemplateRepository.save(any(NotificationTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(emailTemplate.getIsActive());

        // Act
        NotificationTemplate result = notificationTemplateService.toggleTemplateActive(1L, false);

        // Assert
        assertFalse(result.getIsActive());
        verify(notificationTemplateRepository).findById(1L);
        verify(notificationTemplateRepository).save(emailTemplate);
    }

    @Test
    void toggleTemplateActive_NotFound() {
        // Arrange
        when(notificationTemplateRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> notificationTemplateService.toggleTemplateActive(999L, true));
        verify(notificationTemplateRepository).findById(999L);
        verify(notificationTemplateRepository, never()).save(any());
    }

    @Test
    void isTemplateNameUnique_NewTemplate() {
        // Arrange
        when(notificationTemplateRepository.findByNameAndCompany_Id("New Template", 1L))
                .thenReturn(Optional.empty());

        // Act
        boolean result = notificationTemplateService.isTemplateNameUnique("New Template", 1L, null);

        // Assert
        assertTrue(result);
        verify(notificationTemplateRepository).findByNameAndCompany_Id("New Template", 1L);
    }

    @Test
    void isTemplateNameUnique_ExistingTemplate_DifferentId() {
        // Arrange
        when(notificationTemplateRepository.findByNameAndCompany_Id("Welcome Email", 1L))
                .thenReturn(Optional.of(emailTemplate));

        // Act
        boolean result = notificationTemplateService.isTemplateNameUnique("Welcome Email", 1L, 999L);

        // Assert
        assertFalse(result);
        verify(notificationTemplateRepository).findByNameAndCompany_Id("Welcome Email", 1L);
    }

    @Test
    void isTemplateNameUnique_ExistingTemplate_SameId() {
        // Arrange
        when(notificationTemplateRepository.findByNameAndCompany_Id("Welcome Email", 1L))
                .thenReturn(Optional.of(emailTemplate));

        // Act
        boolean result = notificationTemplateService.isTemplateNameUnique("Welcome Email", 1L, 1L);

        // Assert
        assertTrue(result);
        verify(notificationTemplateRepository).findByNameAndCompany_Id("Welcome Email", 1L);
    }

    @Test
    void previewTemplate_Success() {
        // Arrange
        when(notificationTemplateRepository.findById(1L))
                .thenReturn(Optional.of(emailTemplate));

        Map<String, String> sampleVariables = Map.of(
                "customerName", "Preview User",
                "companyName", "Preview Company",
                "code", "PREVIEW123");

        // Act
        Map<String, String> result = notificationTemplateService.previewTemplate(1L, sampleVariables);

        // Assert
        assertNotNull(result);
        assertEquals("Welcome Preview User!", result.get("subject"));
        assertEquals("Hi Preview User, welcome to Preview Company. Your code: PREVIEW123", result.get("body"));
        assertEquals("<h1>Hi Preview User</h1><p>Welcome to Preview Company</p>", result.get("htmlBody"));
        verify(notificationTemplateRepository).findById(1L);
    }

    @Test
    void previewTemplate_NoHtmlBody() {
        // Arrange
        when(notificationTemplateRepository.findById(2L))
                .thenReturn(Optional.of(smsTemplate));

        Map<String, String> sampleVariables = Map.of("name", "Test", "code", "TEST123");

        // Act
        Map<String, String> result = notificationTemplateService.previewTemplate(2L, sampleVariables);

        // Assert
        assertNotNull(result);
        assertEquals("", result.get("subject"));
        assertEquals("Hi Test, use code TEST123 for 20% off!", result.get("body"));
        assertNull(result.get("htmlBody"));
        verify(notificationTemplateRepository).findById(2L);
    }

    @Test
    void previewTemplate_NotFound() {
        // Arrange
        when(notificationTemplateRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> notificationTemplateService.previewTemplate(999L, Map.of()));
        verify(notificationTemplateRepository).findById(999L);
    }

    @Test
    void extractVariables_Success() {
        // Arrange
        String content = "Hello {{name}}, your code is {{code}}. Welcome to {{company}}!";

        // Act
        List<String> result = notificationTemplateService.extractVariables(content);

        // Assert
        assertEquals(3, result.size());
        assertTrue(result.contains("name"));
        assertTrue(result.contains("code"));
        assertTrue(result.contains("company"));
    }

    @Test
    void extractVariables_NoVariables() {
        // Arrange
        String content = "Hello, this is a plain text message.";

        // Act
        List<String> result = notificationTemplateService.extractVariables(content);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void extractVariables_NullContent() {
        // Act
        List<String> result = notificationTemplateService.extractVariables(null);

        // Assert
        assertTrue(result.isEmpty());
    }
}
