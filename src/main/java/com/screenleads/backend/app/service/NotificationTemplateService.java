package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.NotificationChannel;
import com.screenleads.backend.app.domain.model.NotificationTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing notification templates
 */
public interface NotificationTemplateService {

    /**
     * Create a new notification template
     */
    NotificationTemplate createTemplate(NotificationTemplate template);

    /**
     * Update an existing notification template
     */
    NotificationTemplate updateTemplate(Long id, NotificationTemplate template);

    /**
     * Delete a notification template
     */
    void deleteTemplate(Long id);

    /**
     * Get template by ID
     */
    Optional<NotificationTemplate> getTemplateById(Long id);

    /**
     * Get all templates for a company
     */
    List<NotificationTemplate> getTemplatesByCompany(Long companyId);

    /**
     * Get active templates for a company
     */
    List<NotificationTemplate> getActiveTemplatesByCompany(Long companyId);

    /**
     * Get templates by channel for a company
     */
    List<NotificationTemplate> getTemplatesByCompanyAndChannel(Long companyId, NotificationChannel channel);

    /**
     * Get most used templates for a company
     */
    List<NotificationTemplate> getMostUsedTemplates(Long companyId, int limit);

    /**
     * Render template with variable substitution
     * @param template The template to render
     * @param variables Map of variable names to values
     * @return Rendered content with variables replaced
     */
    String renderTemplateBody(NotificationTemplate template, Map<String, String> variables);

    /**
     * Render template subject with variable substitution
     */
    String renderTemplateSubject(NotificationTemplate template, Map<String, String> variables);

    /**
     * Increment usage count for a template
     */
    void incrementUsageCount(Long templateId);

    /**
     * Toggle template active status
     */
    NotificationTemplate toggleTemplateActive(Long id, boolean active);

    /**
     * Check if template name is unique for company
     */
    boolean isTemplateNameUnique(String name, Long companyId, Long excludeId);

    /**
     * Preview rendered template with sample variables
     */
    Map<String, String> previewTemplate(Long templateId, Map<String, String> sampleVariables);
}
