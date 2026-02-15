package com.screenleads.backend.app.controller;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.NotificationChannel;
import com.screenleads.backend.app.domain.model.NotificationTemplate;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.dto.request.NotificationTemplateRequest;
import com.screenleads.backend.app.service.NotificationTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing notification templates
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;
    private final CompanyRepository companyRepository;

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createTemplate(@Valid @RequestBody NotificationTemplateRequest request) {
        try {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found: " + request.getCompanyId()));

            if (!notificationTemplateService.isTemplateNameUnique(request.getName(), request.getCompanyId(), null)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Template name already exists for this company"));
            }

            NotificationTemplate template = NotificationTemplate.builder()
                    .company(company)
                    .name(request.getName())
                    .description(request.getDescription())
                    .channel(request.getChannel())
                    .subject(request.getSubject())
                    .body(request.getBody())
                    .htmlBody(request.getHtmlBody())
                    .availableVariables(request.getAvailableVariables())
                    .isActive(request.getIsActive())
                    .sender(request.getSender())
                    .replyTo(request.getReplyTo())
                    .createdBy(request.getCreatedBy())
                    .metadata(request.getMetadata())
                    .build();

            NotificationTemplate created = notificationTemplateService.createTemplate(template);
            log.info("Created notification template ID {} for company ID {}", created.getId(), request.getCompanyId());

            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (RuntimeException e) {
            log.error("Error creating notification template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','update')")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        try {
            NotificationTemplate existing = notificationTemplateService.getTemplateById(id)
                    .orElseThrow(() -> new RuntimeException("Notification template not found: " + id));

            if (!notificationTemplateService.isTemplateNameUnique(request.getName(),
                    existing.getCompany().getId(), id)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Template name already exists for this company"));
            }

            NotificationTemplate template = NotificationTemplate.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .channel(request.getChannel())
                    .subject(request.getSubject())
                    .body(request.getBody())
                    .htmlBody(request.getHtmlBody())
                    .availableVariables(request.getAvailableVariables())
                    .isActive(request.getIsActive())
                    .sender(request.getSender())
                    .replyTo(request.getReplyTo())
                    .metadata(request.getMetadata())
                    .build();

            NotificationTemplate updated = notificationTemplateService.updateTemplate(id, template);
            log.info("Updated notification template ID {}", id);

            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            log.error("Error updating notification template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','delete')")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        try {
            notificationTemplateService.deleteTemplate(id);
            log.info("Deleted notification template ID {}", id);
            return ResponseEntity.ok(Map.of("message", "Template deleted successfully"));

        } catch (Exception e) {
            log.error("Error deleting notification template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTemplateById(@PathVariable Long id) {
        return notificationTemplateService.getTemplateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/company/{companyId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationTemplate>> getTemplatesByCompany(
            @PathVariable Long companyId,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        List<NotificationTemplate> templates;

        if (channel != null) {
            templates = notificationTemplateService.getTemplatesByCompanyAndChannel(companyId, channel);
        } else if (activeOnly) {
            templates = notificationTemplateService.getActiveTemplatesByCompany(companyId);
        } else {
            templates = notificationTemplateService.getTemplatesByCompany(companyId);
        }

        return ResponseEntity.ok(templates);
    }

    @GetMapping("/company/{companyId}/most-used")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','read')")
    public ResponseEntity<List<NotificationTemplate>> getMostUsedTemplates(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "10") int limit) {

        List<NotificationTemplate> templates = notificationTemplateService.getMostUsedTemplates(companyId, limit);
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/{id}/preview")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> previewTemplate(@PathVariable Long id,
            @RequestBody Map<String, String> sampleVariables) {
        try {
            Map<String, String> preview = notificationTemplateService.previewTemplate(id, sampleVariables);
            return ResponseEntity.ok(preview);

        } catch (Exception e) {
            log.error("Error previewing template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/preview")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> previewTemplateContent(@RequestBody Map<String, Object> request) {
        try {
            String subject = (String) request.get("subject");
            String body = (String) request.get("body");
            @SuppressWarnings("unchecked")
            Map<String, String> variables = (Map<String, String>) request.get("variables");

            if (body == null || body.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Body is required"));
            }

            // Replace variables in subject and body
            String processedSubject = subject != null ? replaceVariables(subject, variables) : "";
            String processedBody = replaceVariables(body, variables);

            Map<String, String> preview = new HashMap<>();
            preview.put("subject", processedSubject);
            preview.put("body", processedBody);
            preview.put("htmlBody", processedBody); // Same content for now

            return ResponseEntity.ok(preview);

        } catch (Exception e) {
            log.error("Error previewing template content: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String replaceVariables(String content, Map<String, String> variables) {
        if (content == null || variables == null) {
            return content;
        }
        
        String result = content;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }

    @PatchMapping("/{id}/toggle")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','update')")
    public ResponseEntity<?> toggleTemplateActive(@PathVariable Long id,
            @RequestParam boolean active) {
        try {
            NotificationTemplate updated = notificationTemplateService.toggleTemplateActive(id, active);

            Map<String, Object> response = new HashMap<>();
            response.put("templateId", id);
            response.put("isActive", updated.getIsActive());
            response.put("message", "Template " + (active ? "activated" : "deactivated") + " successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error toggling template active status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
