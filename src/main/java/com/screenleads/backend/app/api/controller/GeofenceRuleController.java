package com.screenleads.backend.app.api.controller;

import com.screenleads.backend.app.domain.model.GeofenceRule;
import com.screenleads.backend.app.service.GeofenceRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for geofence rule management
 */
@RestController
@RequestMapping("/api/geofence/rules")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class GeofenceRuleController {

    private final GeofenceRuleService geofenceRuleService;

    /**
     * Create a new geofence rule
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<GeofenceRule> createRule(@Valid @RequestBody GeofenceRule rule) {
        log.info("Creating new geofence rule for zone: {}", rule.getZone().getId());
        GeofenceRule created = geofenceRuleService.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing geofence rule
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<GeofenceRule> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody GeofenceRule rule) {
        log.info("Updating geofence rule: {}", id);
        GeofenceRule updated = geofenceRuleService.updateRule(id, rule);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a geofence rule
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        log.info("Deleting geofence rule: {}", id);
        geofenceRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get geofence rule by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<GeofenceRule> getRuleById(@PathVariable Long id) {
        return geofenceRuleService.getRuleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all rules for a company
     */
    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceRule>> getRulesByCompany(@PathVariable Long companyId) {
        log.info("Getting all geofence rules for company: {}", companyId);
        List<GeofenceRule> rules = geofenceRuleService.getRulesByCompany(companyId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get active rules for a company
     */
    @GetMapping("/company/{companyId}/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceRule>> getActiveRulesByCompany(@PathVariable Long companyId) {
        log.info("Getting active geofence rules for company: {}", companyId);
        List<GeofenceRule> rules = geofenceRuleService.getActiveRulesByCompany(companyId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get rules for a specific zone
     */
    @GetMapping("/zone/{zoneId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceRule>> getRulesByZone(@PathVariable Long zoneId) {
        log.info("Getting geofence rules for zone: {}", zoneId);
        List<GeofenceRule> rules = geofenceRuleService.getRulesByZone(zoneId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get active rules for a specific zone
     */
    @GetMapping("/zone/{zoneId}/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceRule>> getActiveRulesByZone(@PathVariable Long zoneId) {
        log.info("Getting active geofence rules for zone: {}", zoneId);
        List<GeofenceRule> rules = geofenceRuleService.getActiveRulesByZone(zoneId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Activate or deactivate a rule
     */
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<GeofenceRule> toggleRuleActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        log.info("Toggling geofence rule {} active status to: {}", id, active);
        GeofenceRule updated = geofenceRuleService.toggleRuleActive(id, active);
        return ResponseEntity.ok(updated);
    }

    /**
     * Find applicable rules for a zone
     */
    @GetMapping("/zone/{zoneId}/applicable")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceRule>> findApplicableRules(@PathVariable Long zoneId) {
        log.info("Finding applicable rules for zone: {}", zoneId);
        List<GeofenceRule> rules = geofenceRuleService.findApplicableRules(zoneId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Count active rules for a company
     */
    @GetMapping("/company/{companyId}/count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Long> countActiveRules(@PathVariable Long companyId) {
        Long count = geofenceRuleService.countActiveRules(companyId);
        return ResponseEntity.ok(count);
    }
}
