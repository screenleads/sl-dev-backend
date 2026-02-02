package com.screenleads.backend.app.controller;

import com.screenleads.backend.app.domain.model.FraudRule;
import com.screenleads.backend.app.domain.model.FraudRuleType;
import com.screenleads.backend.app.domain.model.FraudSeverity;
import com.screenleads.backend.app.service.FraudRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Fraud Detection Rules
 */
@RestController
@RequestMapping("/api/fraud-rules")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins:*}")
public class FraudRuleController {

    private final FraudRuleService fraudRuleService;

    /**
     * Get all fraud rules by company
     */
    @GetMapping("/company/{companyId}")
    @PreAuthorize("@perm.can('fraud_detection', 'read')")
    public ResponseEntity<List<FraudRule>> getRulesByCompany(@PathVariable Long companyId) {
        log.info("GET /api/fraud-rules/company/{} - Getting fraud rules", companyId);
        List<FraudRule> rules = fraudRuleService.getRulesByCompany(companyId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get active fraud rules by company
     */
    @GetMapping("/company/{companyId}/active")
    @PreAuthorize("@perm.can('fraud_detection', 'read')")
    public ResponseEntity<List<FraudRule>> getActiveRulesByCompany(@PathVariable Long companyId) {
        log.info("GET /api/fraud-rules/company/{}/active - Getting active fraud rules", companyId);
        List<FraudRule> rules = fraudRuleService.getActiveRulesByCompany(companyId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get fraud rules by type
     */
    @GetMapping("/company/{companyId}/type/{ruleType}")
    @PreAuthorize("@perm.can('fraud_detection', 'read')")
    public ResponseEntity<List<FraudRule>> getRulesByType(
            @PathVariable Long companyId,
            @PathVariable FraudRuleType ruleType) {
        log.info("GET /api/fraud-rules/company/{}/type/{} - Getting fraud rules by type",
                companyId, ruleType);
        List<FraudRule> rules = fraudRuleService.getRulesByType(companyId, ruleType);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get fraud rules by severity
     */
    @GetMapping("/company/{companyId}/severity/{severity}")
    @PreAuthorize("@perm.can('fraud_detection', 'read')")
    public ResponseEntity<List<FraudRule>> getRulesBySeverity(
            @PathVariable Long companyId,
            @PathVariable FraudSeverity severity) {
        log.info("GET /api/fraud-rules/company/{}/severity/{} - Getting fraud rules by severity",
                companyId, severity);
        List<FraudRule> rules = fraudRuleService.getRulesBySeverity(companyId, severity);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get auto-block rules by company
     */
    @GetMapping("/company/{companyId}/auto-block")
    @PreAuthorize("@perm.can('fraud_detection', 'read')")
    public ResponseEntity<List<FraudRule>> getAutoBlockRulesByCompany(@PathVariable Long companyId) {
        log.info("GET /api/fraud-rules/company/{}/auto-block - Getting auto-block rules", companyId);
        List<FraudRule> rules = fraudRuleService.getAutoBlockRulesByCompany(companyId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get fraud rule by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("@perm.can('fraud_detection', 'read')")
    public ResponseEntity<FraudRule> getRuleById(@PathVariable Long id) {
        log.info("GET /api/fraud-rules/{} - Getting fraud rule", id);
        return fraudRuleService.getRuleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new fraud rule
     */
    @PostMapping
    @PreAuthorize("@perm.can('fraud_detection', 'create')")
    public ResponseEntity<FraudRule> createRule(@RequestBody FraudRule rule) {
        log.info("POST /api/fraud-rules - Creating fraud rule: {}", rule.getName());
        FraudRule createdRule = fraudRuleService.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRule);
    }

    /**
     * Update an existing fraud rule
     */
    @PutMapping("/{id}")
    @PreAuthorize("@perm.can('fraud_detection', 'update')")
    public ResponseEntity<FraudRule> updateRule(
            @PathVariable Long id,
            @RequestBody FraudRule ruleDetails) {
        log.info("PUT /api/fraud-rules/{} - Updating fraud rule", id);
        FraudRule updatedRule = fraudRuleService.updateRule(id, ruleDetails);
        return ResponseEntity.ok(updatedRule);
    }

    /**
     * Delete a fraud rule
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.can('fraud_detection', 'delete')")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        log.info("DELETE /api/fraud-rules/{} - Deleting fraud rule", id);
        fraudRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle fraud rule active status
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("@perm.can('fraud_detection', 'update')")
    public ResponseEntity<FraudRule> toggleRuleActive(@PathVariable Long id) {
        log.info("PATCH /api/fraud-rules/{}/toggle-active - Toggling rule active status", id);
        FraudRule updatedRule = fraudRuleService.toggleRuleActive(id);
        return ResponseEntity.ok(updatedRule);
    }

    /**
     * Count active rules by company
     */
    @GetMapping("/company/{companyId}/count")
    @PreAuthorize("@perm.can('fraud_detection', 'read')")
    public ResponseEntity<Long> countActiveRulesByCompany(@PathVariable Long companyId) {
        log.info("GET /api/fraud-rules/company/{}/count - Counting active fraud rules", companyId);
        long count = fraudRuleService.countActiveRulesByCompany(companyId);
        return ResponseEntity.ok(count);
    }

    /**
     * Check if rule name exists
     */
    @GetMapping("/company/{companyId}/exists")
    @PreAuthorize("@perm.can('fraud_detection', 'read')")
    public ResponseEntity<Boolean> ruleNameExists(
            @PathVariable Long companyId,
            @RequestParam String name) {
        log.info("GET /api/fraud-rules/company/{}/exists?name={} - Checking if rule name exists",
                companyId, name);
        boolean exists = fraudRuleService.ruleNameExists(name, companyId);
        return ResponseEntity.ok(exists);
    }
}
