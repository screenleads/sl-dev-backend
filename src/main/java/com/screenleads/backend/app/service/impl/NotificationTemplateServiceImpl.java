package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.NotificationChannel;
import com.screenleads.backend.app.domain.model.NotificationTemplate;
import com.screenleads.backend.app.domain.repository.NotificationTemplateRepository;
import com.screenleads.backend.app.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository notificationTemplateRepository;

    // Pattern to match variables in format: {{variableName}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    @Override
    @Transactional
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        log.info("Creating new notification template: {} for company ID: {}",
                template.getName(), template.getCompany().getId());

        return notificationTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public NotificationTemplate updateTemplate(Long id, NotificationTemplate template) {
        NotificationTemplate existing = notificationTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification template not found: " + id));

        log.info("Updating notification template ID: {}", id);

        existing.setName(template.getName());
        existing.setDescription(template.getDescription());
        existing.setChannel(template.getChannel());
        existing.setSubject(template.getSubject());
        existing.setBody(template.getBody());
        existing.setHtmlBody(template.getHtmlBody());
        existing.setAvailableVariables(template.getAvailableVariables());
        existing.setIsActive(template.getIsActive());
        existing.setSender(template.getSender());
        existing.setReplyTo(template.getReplyTo());
        existing.setMetadata(template.getMetadata());

        return notificationTemplateRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        log.info("Deleting notification template ID: {}", id);
        notificationTemplateRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationTemplate> getTemplateById(Long id) {
        return notificationTemplateRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getTemplatesByCompany(Long companyId) {
        return notificationTemplateRepository.findByCompany_IdOrderByCreatedAtDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getActiveTemplatesByCompany(Long companyId) {
        return notificationTemplateRepository.findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getTemplatesByCompanyAndChannel(Long companyId, NotificationChannel channel) {
        return notificationTemplateRepository.findByCompany_IdAndChannelAndIsActiveTrueOrderByCreatedAtDesc(
                companyId, channel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getMostUsedTemplates(Long companyId, int limit) {
        return notificationTemplateRepository.findMostUsedByCompanyId(companyId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public String renderTemplateBody(NotificationTemplate template, Map<String, String> variables) {
        if (template.getBody() == null) {
            return "";
        }
        return replaceVariables(template.getBody(), variables);
    }

    @Override
    @Transactional(readOnly = true)
    public String renderTemplateSubject(NotificationTemplate template, Map<String, String> variables) {
        if (template.getSubject() == null) {
            return "";
        }
        return replaceVariables(template.getSubject(), variables);
    }

    @Override
    @Transactional
    public void incrementUsageCount(Long templateId) {
        NotificationTemplate template = notificationTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        template.setUsageCount(template.getUsageCount() + 1);
        template.setLastUsedAt(LocalDateTime.now());

        notificationTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public NotificationTemplate toggleTemplateActive(Long id, boolean active) {
        NotificationTemplate template = notificationTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification template not found: " + id));

        template.setIsActive(active);
        return notificationTemplateRepository.save(template);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTemplateNameUnique(String name, Long companyId, Long excludeId) {
        Optional<NotificationTemplate> existing = notificationTemplateRepository.findByNameAndCompany_Id(name,
                companyId);

        if (existing.isEmpty()) {
            return true;
        }

        // If updating, check if it's the same template
        return excludeId != null && existing.get().getId().equals(excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> previewTemplate(Long templateId, Map<String, String> sampleVariables) {
        NotificationTemplate template = notificationTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        Map<String, String> preview = new HashMap<>();
        preview.put("subject", renderTemplateSubject(template, sampleVariables));
        preview.put("body", renderTemplateBody(template, sampleVariables));

        if (template.getHtmlBody() != null && !template.getHtmlBody().isEmpty()) {
            preview.put("htmlBody", replaceVariables(template.getHtmlBody(), sampleVariables));
        }

        return preview;
    }

    /**
     * Replace variables in a string with their values
     * Variables are in the format: {{variableName}}
     */
    private String replaceVariables(String content, Map<String, String> variables) {
        if (content == null || variables == null) {
            return content;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = variables.getOrDefault(variableName, "{{" + variableName + "}}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Extract all variable names from a template string
     */
    public List<String> extractVariables(String content) {
        if (content == null) {
            return Collections.emptyList();
        }

        List<String> variables = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);

        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }
}
