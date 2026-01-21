package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.GeofenceRule;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing geofence rules
 */
public interface GeofenceRuleService {

    /**
     * Create a new geofence rule
     */
    GeofenceRule createRule(GeofenceRule rule);

    /**
     * Update an existing geofence rule
     */
    GeofenceRule updateRule(Long id, GeofenceRule rule);

    /**
     * Delete a geofence rule
     */
    void deleteRule(Long id);

    /**
     * Get geofence rule by ID
     */
    Optional<GeofenceRule> getRuleById(Long id);

    /**
     * Get all rules for a company
     */
    List<GeofenceRule> getRulesByCompany(Long companyId);

    /**
     * Get active rules for a company
     */
    List<GeofenceRule> getActiveRulesByCompany(Long companyId);

    /**
     * Get rules for a specific zone
     */
    List<GeofenceRule> getRulesByZone(Long zoneId);

    /**
     * Get active rules for a specific zone
     */
    List<GeofenceRule> getActiveRulesByZone(Long zoneId);

    /**
     * Activate or deactivate a rule
     */
    GeofenceRule toggleRuleActive(Long id, boolean active);

    /**
     * Find applicable rules for a zone entry/exit
     */
    List<GeofenceRule> findApplicableRules(Long zoneId);

    /**
     * Count active rules for a company
     */
    Long countActiveRules(Long companyId);
}
