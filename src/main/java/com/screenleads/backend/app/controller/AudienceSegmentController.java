package com.screenleads.backend.app.controller;

import com.screenleads.backend.app.domain.model.AudienceSegment;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.dto.request.AudienceSegmentRequest;
import com.screenleads.backend.app.service.AudienceSegmentService;
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
 * Controller for managing audience segments
 */
@RestController
@RequestMapping("/api/audiences")
@RequiredArgsConstructor
@Slf4j
public class AudienceSegmentController {

    private final AudienceSegmentService audienceSegmentService;
    private final CompanyRepository companyRepository;

    /**
     * Create a new audience segment
     */
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','create')")
    public ResponseEntity<?> createSegment(@Valid @RequestBody AudienceSegmentRequest request) {
        try {
            // Validate company exists
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found: " + request.getCompanyId()));

            // Check name uniqueness
            if (!audienceSegmentService.isSegmentNameUnique(request.getName(), request.getCompanyId(), null)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Segment name already exists for this company"));
            }

            AudienceSegment segment = AudienceSegment.builder()
                    .company(company)
                    .name(request.getName())
                    .description(request.getDescription())
                    .filterRules(request.getFilterRules())
                    .isActive(request.getIsActive())
                    .createdBy(request.getCreatedBy())
                    .metadata(request.getMetadata())
                    .build();

            AudienceSegment created = audienceSegmentService.createSegment(segment);

            log.info("Created audience segment ID {} for company ID {}", created.getId(), request.getCompanyId());

            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (RuntimeException e) {
            log.error("Error creating audience segment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update an existing audience segment
     */
    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','update')")
    public ResponseEntity<?> updateSegment(@PathVariable Long id,
            @Valid @RequestBody AudienceSegmentRequest request) {
        try {
            // Validate segment exists
            AudienceSegment existing = audienceSegmentService.getSegmentById(id)
                    .orElseThrow(() -> new RuntimeException("Audience segment not found: " + id));

            // Check name uniqueness
            if (!audienceSegmentService.isSegmentNameUnique(request.getName(),
                    existing.getCompany().getId(), id)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Segment name already exists for this company"));
            }

            AudienceSegment segment = AudienceSegment.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .filterRules(request.getFilterRules())
                    .isActive(request.getIsActive())
                    .metadata(request.getMetadata())
                    .build();

            AudienceSegment updated = audienceSegmentService.updateSegment(id, segment);

            log.info("Updated audience segment ID {}", id);

            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            log.error("Error updating audience segment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete an audience segment
     */
    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','delete')")
    public ResponseEntity<?> deleteSegment(@PathVariable Long id) {
        try {
            audienceSegmentService.deleteSegment(id);
            log.info("Deleted audience segment ID {}", id);
            return ResponseEntity.ok(Map.of("message", "Segment deleted successfully"));

        } catch (Exception e) {
            log.error("Error deleting audience segment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get audience segment by ID
     */
    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','read')")
    public ResponseEntity<?> getSegmentById(@PathVariable Long id) {
        return audienceSegmentService.getSegmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all audience segments for a company
     */
    @GetMapping("/company/{companyId}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','read')")
    public ResponseEntity<List<AudienceSegment>> getSegmentsByCompany(@PathVariable Long companyId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<AudienceSegment> segments;

        if (activeOnly) {
            segments = audienceSegmentService.getActiveSegmentsByCompany(companyId);
        } else {
            segments = audienceSegmentService.getSegmentsByCompany(companyId);
        }

        return ResponseEntity.ok(segments);
    }

    /**
     * Get customers matching a segment
     */
    @GetMapping("/{id}/customers")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','read')")
    public ResponseEntity<List<Customer>> getSegmentCustomers(@PathVariable Long id) {
        try {
            List<Customer> customers = audienceSegmentService.getCustomersInSegment(id);
            return ResponseEntity.ok(customers);

        } catch (RuntimeException e) {
            log.error("Error getting segment customers: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Preview customers that would match segment rules (without saving)
     */
    @PostMapping("/preview")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('remarketing','read')")
    public ResponseEntity<?> previewSegment(@Valid @RequestBody AudienceSegmentRequest request) {
        try {
            AudienceSegment segment = AudienceSegment.builder()
                    .filterRules(request.getFilterRules())
                    .build();

            List<Customer> matchingCustomers = audienceSegmentService.previewSegmentMatch(
                    request.getCompanyId(), segment);

            Map<String, Object> response = new HashMap<>();
            response.put("matchingCustomers", matchingCustomers);
            response.put("count", matchingCustomers.size());
            response.put("message", "Preview limited to 100 customers");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error previewing segment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Recalculate customer count for a specific segment
     */
    @PostMapping("/{id}/recalculate")
    public ResponseEntity<?> recalculateSegmentCount(@PathVariable Long id) {
        try {
            audienceSegmentService.updateSegmentCustomerCount(id);

            AudienceSegment updated = audienceSegmentService.getSegmentById(id)
                    .orElseThrow(() -> new RuntimeException("Segment not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("segmentId", id);
            response.put("customerCount", updated.getCustomerCount());
            response.put("lastCalculatedAt", updated.getLastCalculatedAt());
            response.put("message", "Customer count recalculated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error recalculating segment count: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Toggle segment active status
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleSegmentActive(@PathVariable Long id,
            @RequestParam boolean active) {
        try {
            AudienceSegment updated = audienceSegmentService.toggleSegmentActive(id, active);

            Map<String, Object> response = new HashMap<>();
            response.put("segmentId", id);
            response.put("isActive", updated.getIsActive());
            response.put("message", "Segment " + (active ? "activated" : "deactivated") + " successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error toggling segment active status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Trigger recalculation of all segment counts (admin endpoint)
     */
    @PostMapping("/recalculate-all")
    public ResponseEntity<Map<String, Object>> recalculateAllSegments() {
        try {
            audienceSegmentService.recalculateAllSegmentCounts();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "All segment counts recalculated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error recalculating all segments: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to recalculate segments: " + e.getMessage()));
        }
    }
}
