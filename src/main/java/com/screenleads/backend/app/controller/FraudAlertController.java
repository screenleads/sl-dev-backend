package com.screenleads.backend.app.controller;

import com.screenleads.backend.app.domain.model.FraudAlert;
import com.screenleads.backend.app.domain.model.FraudAlertStatus;
import com.screenleads.backend.app.domain.model.FraudSeverity;
import com.screenleads.backend.app.service.FraudAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Fraud Detection Alerts
 */
@RestController
@RequestMapping("/api/fraud-alerts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins:*}")
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    /**
     * Get all fraud alerts by company
     */
    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<List<FraudAlert>> getAlertsByCompany(@PathVariable Long companyId) {
        log.info("GET /api/fraud-alerts/company/{} - Getting fraud alerts", companyId);
        List<FraudAlert> alerts = fraudAlertService.getAlertsByCompany(companyId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get fraud alerts by rule
     */
    @GetMapping("/rule/{ruleId}")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<List<FraudAlert>> getAlertsByRule(@PathVariable Long ruleId) {
        log.info("GET /api/fraud-alerts/rule/{} - Getting fraud alerts by rule", ruleId);
        List<FraudAlert> alerts = fraudAlertService.getAlertsByRule(ruleId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get fraud alerts by status
     */
    @GetMapping("/company/{companyId}/status/{status}")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<List<FraudAlert>> getAlertsByStatus(
            @PathVariable Long companyId,
            @PathVariable FraudAlertStatus status) {
        log.info("GET /api/fraud-alerts/company/{}/status/{} - Getting alerts by status",
                companyId, status);
        List<FraudAlert> alerts = fraudAlertService.getAlertsByStatus(companyId, status);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get fraud alerts by severity
     */
    @GetMapping("/company/{companyId}/severity/{severity}")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<List<FraudAlert>> getAlertsBySeverity(
            @PathVariable Long companyId,
            @PathVariable FraudSeverity severity) {
        log.info("GET /api/fraud-alerts/company/{}/severity/{} - Getting alerts by severity",
                companyId, severity);
        List<FraudAlert> alerts = fraudAlertService.getAlertsBySeverity(companyId, severity);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get pending fraud alerts
     */
    @GetMapping("/company/{companyId}/pending")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<List<FraudAlert>> getPendingAlertsByCompany(@PathVariable Long companyId) {
        log.info("GET /api/fraud-alerts/company/{}/pending - Getting pending alerts", companyId);
        List<FraudAlert> alerts = fraudAlertService.getPendingAlertsByCompany(companyId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get recent fraud alerts (last N days)
     */
    @GetMapping("/company/{companyId}/recent")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<List<FraudAlert>> getRecentAlertsByCompany(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "7") int days) {
        log.info("GET /api/fraud-alerts/company/{}/recent?days={} - Getting recent alerts",
                companyId, days);
        List<FraudAlert> alerts = fraudAlertService.getRecentAlertsByCompany(companyId, days);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get high priority fraud alerts
     */
    @GetMapping("/company/{companyId}/high-priority")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<List<FraudAlert>> getHighPriorityAlertsByCompany(@PathVariable Long companyId) {
        log.info("GET /api/fraud-alerts/company/{}/high-priority - Getting high priority alerts",
                companyId);
        List<FraudAlert> alerts = fraudAlertService.getHighPriorityAlertsByCompany(companyId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alerts by related entity
     */
    @GetMapping("/company/{companyId}/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<List<FraudAlert>> getAlertsByRelatedEntity(
            @PathVariable Long companyId,
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        log.info("GET /api/fraud-alerts/company/{}/entity/{}/{} - Getting alerts by entity",
                companyId, entityType, entityId);
        List<FraudAlert> alerts = fraudAlertService.getAlertsByRelatedEntity(
                companyId, entityType, entityId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get fraud alert by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<FraudAlert> getAlertById(@PathVariable Long id) {
        log.info("GET /api/fraud-alerts/{} - Getting fraud alert", id);
        return fraudAlertService.getAlertById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new fraud alert
     */
    @PostMapping
    @PreAuthorize("hasAuthority('fraud-alerts:create')")
    public ResponseEntity<FraudAlert> createAlert(@RequestBody FraudAlert alert) {
        log.info("POST /api/fraud-alerts - Creating fraud alert");
        FraudAlert createdAlert = fraudAlertService.createAlert(alert);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAlert);
    }

    /**
     * Update fraud alert status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('fraud-alerts:update')")
    public ResponseEntity<FraudAlert> updateAlertStatus(
            @PathVariable Long id,
            @RequestParam FraudAlertStatus status) {
        log.info("PATCH /api/fraud-alerts/{}/status - Updating alert status to: {}", id, status);
        FraudAlert updatedAlert = fraudAlertService.updateAlertStatus(id, status);
        return ResponseEntity.ok(updatedAlert);
    }

    /**
     * Resolve a fraud alert
     */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('fraud-alerts:update')")
    public ResponseEntity<FraudAlert> resolveAlert(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        log.info("POST /api/fraud-alerts/{}/resolve - Resolving fraud alert", id);

        Long userId = ((Number) payload.get("userId")).longValue();
        String resolutionNotes = (String) payload.get("resolutionNotes");

        FraudAlert resolvedAlert = fraudAlertService.resolveAlert(id, userId, resolutionNotes);
        return ResponseEntity.ok(resolvedAlert);
    }

    /**
     * Update a fraud alert
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('fraud-alerts:update')")
    public ResponseEntity<FraudAlert> updateAlert(
            @PathVariable Long id,
            @RequestBody FraudAlert alertDetails) {
        log.info("PUT /api/fraud-alerts/{} - Updating fraud alert", id);
        FraudAlert updatedAlert = fraudAlertService.updateAlert(id, alertDetails);
        return ResponseEntity.ok(updatedAlert);
    }

    /**
     * Delete a fraud alert
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('fraud-alerts:delete')")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        log.info("DELETE /api/fraud-alerts/{} - Deleting fraud alert", id);
        fraudAlertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get alert statistics
     */
    @GetMapping("/company/{companyId}/statistics")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<Map<String, Long>> getAlertStatistics(@PathVariable Long companyId) {
        log.info("GET /api/fraud-alerts/company/{}/statistics - Getting alert statistics", companyId);
        Map<String, Long> stats = fraudAlertService.getAlertStatistics(companyId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Count alerts by status
     */
    @GetMapping("/company/{companyId}/count/status/{status}")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<Long> countAlertsByStatus(
            @PathVariable Long companyId,
            @PathVariable FraudAlertStatus status) {
        log.info("GET /api/fraud-alerts/company/{}/count/status/{} - Counting alerts",
                companyId, status);
        long count = fraudAlertService.countAlertsByStatus(companyId, status);
        return ResponseEntity.ok(count);
    }

    /**
     * Count alerts by severity
     */
    @GetMapping("/company/{companyId}/count/severity/{severity}")
    @PreAuthorize("hasAuthority('fraud-alerts:read')")
    public ResponseEntity<Long> countAlertsBySeverity(
            @PathVariable Long companyId,
            @PathVariable FraudSeverity severity) {
        log.info("GET /api/fraud-alerts/company/{}/count/severity/{} - Counting alerts",
                companyId, severity);
        long count = fraudAlertService.countAlertsBySeverity(companyId, severity);
        return ResponseEntity.ok(count);
    }
}
