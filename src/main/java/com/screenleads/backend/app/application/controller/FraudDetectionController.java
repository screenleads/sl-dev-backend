package com.screenleads.backend.app.application.controller;

import com.screenleads.backend.app.application.service.FraudDetectionService;
import com.screenleads.backend.app.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionController {

    private final FraudDetectionService fraudService;

    // ========== Rule Management ==========

    @PostMapping("/rules")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudRule> createRule(@RequestBody FraudRule rule) {
        log.info("POST /api/fraud/rules - Creating rule: {}", rule.getName());
        FraudRule created = fraudService.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/rules/{ruleId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudRule> updateRule(
            @PathVariable Long ruleId,
            @RequestBody FraudRule rule) {
        log.info("PUT /api/fraud/rules/{}", ruleId);
        FraudRule updated = fraudService.updateRule(ruleId, rule);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/rules/{ruleId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable Long ruleId) {
        log.info("DELETE /api/fraud/rules/{}", ruleId);
        fraudService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rules/{ruleId}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('fraud','read')")
    public ResponseEntity<FraudRule> getRule(@PathVariable Long ruleId) {
        FraudRule rule = fraudService.getRule(ruleId);
        return ResponseEntity.ok(rule);
    }

    @GetMapping("/rules/company/{companyId}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('fraud','read')")
    public ResponseEntity<List<FraudRule>> getCompanyRules(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<FraudRule> rules = activeOnly
                ? fraudService.getActiveRulesByCompany(companyId)
                : fraudService.getRulesByCompany(companyId);
        return ResponseEntity.ok(rules);
    }

    // ========== Alert Management ==========

    @PostMapping("/alerts")
    public ResponseEntity<FraudAlert> createAlert(@RequestBody FraudAlert alert) {
        log.info("POST /api/fraud/alerts - Creating alert");
        FraudAlert created = fraudService.createAlert(alert);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/alerts/{alertId}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudAlert> updateAlertStatus(
            @PathVariable Long alertId,
            @RequestBody AlertStatusUpdate statusUpdate) {
        log.info("PUT /api/fraud/alerts/{}/status - New status: {}",
                alertId, statusUpdate.getStatus());
        FraudAlert updated = fraudService.updateAlertStatus(
                alertId,
                statusUpdate.getStatus(),
                statusUpdate.getNotes());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/alerts/{alertId}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('fraud','read')")
    public ResponseEntity<FraudAlert> getAlert(@PathVariable Long alertId) {
        FraudAlert alert = fraudService.getAlert(alertId);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/alerts/company/{companyId}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('fraud','read')")
    public ResponseEntity<List<FraudAlert>> getCompanyAlerts(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<FraudAlert> alerts = fraudService.getAlertsByCompany(companyId, page, size);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/company/{companyId}/pending")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('fraud','read')")
    public ResponseEntity<List<FraudAlert>> getPendingAlerts(@PathVariable Long companyId) {
        List<FraudAlert> alerts = fraudService.getPendingAlerts(companyId);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/company/{companyId}/stats")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('fraud','read')")
    public ResponseEntity<Map<String, Object>> getAlertStats(@PathVariable Long companyId) {
        Map<String, Object> stats = fraudService.getAlertStatistics(companyId);
        return ResponseEntity.ok(stats);
    }

    // ========== Blacklist Management ==========

    @PostMapping("/blacklist")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Blacklist> addToBlacklist(@RequestBody Blacklist entry) {
        log.info("POST /api/fraud/blacklist - Type: {}, Value: {}",
                entry.getBlacklistType(), entry.getValue());
        Blacklist created = fraudService.addToBlacklist(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/blacklist/{blacklistId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable Long blacklistId) {
        log.info("DELETE /api/fraud/blacklist/{}", blacklistId);
        fraudService.removeFromBlacklist(blacklistId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blacklist/check")
    public ResponseEntity<Map<String, Boolean>> checkBlacklist(
            @RequestParam Long companyId,
            @RequestParam BlacklistType type,
            @RequestParam String value) {
        boolean isBlacklisted = fraudService.isBlacklisted(companyId, type, value);
        return ResponseEntity.ok(Map.of("isBlacklisted", isBlacklisted));
    }

    @GetMapping("/blacklist/company/{companyId}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('fraud','read')")
    public ResponseEntity<List<Blacklist>> getCompanyBlacklist(@PathVariable Long companyId) {
        List<Blacklist> blacklist = fraudService.getBlacklistByCompany(companyId);
        return ResponseEntity.ok(blacklist);
    }

    // ========== Fraud Detection ==========

    @PostMapping("/check")
    public ResponseEntity<FraudCheckResponse> checkForFraud(@RequestBody FraudCheckRequest request) {
        log.info("POST /api/fraud/check - Entity: {}, ID: {}",
                request.getEntityType(), request.getEntityId());

        List<FraudAlert> alerts = fraudService.checkForFraud(
                request.getCompanyId(),
                request.getEntityType(),
                request.getEntityId(),
                request.getContext());

        FraudCheckResponse response = new FraudCheckResponse();
        response.setFraudDetected(!alerts.isEmpty());
        response.setAlerts(alerts);
        response.setAlertCount(alerts.size());

        return ResponseEntity.ok(response);
    }

    // ========== DTOs ==========

    public static class AlertStatusUpdate {
        private FraudAlertStatus status;
        private String notes;

        public FraudAlertStatus getStatus() {
            return status;
        }

        public void setStatus(FraudAlertStatus status) {
            this.status = status;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class FraudCheckRequest {
        private Long companyId;
        private String entityType;
        private Long entityId;
        private Map<String, Object> context;

        public Long getCompanyId() {
            return companyId;
        }

        public void setCompanyId(Long companyId) {
            this.companyId = companyId;
        }

        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }

        public Long getEntityId() {
            return entityId;
        }

        public void setEntityId(Long entityId) {
            this.entityId = entityId;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }
    }

    public static class FraudCheckResponse {
        private boolean fraudDetected;
        private int alertCount;
        private List<FraudAlert> alerts;

        public boolean isFraudDetected() {
            return fraudDetected;
        }

        public void setFraudDetected(boolean fraudDetected) {
            this.fraudDetected = fraudDetected;
        }

        public int getAlertCount() {
            return alertCount;
        }

        public void setAlertCount(int alertCount) {
            this.alertCount = alertCount;
        }

        public List<FraudAlert> getAlerts() {
            return alerts;
        }

        public void setAlerts(List<FraudAlert> alerts) {
            this.alerts = alerts;
        }
    }
}
