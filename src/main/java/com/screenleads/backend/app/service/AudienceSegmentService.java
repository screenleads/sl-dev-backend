package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.AudienceSegment;
import com.screenleads.backend.app.domain.model.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing audience segments
 */
public interface AudienceSegmentService {

    /**
     * Create a new audience segment
     */
    AudienceSegment createSegment(AudienceSegment segment);

    /**
     * Update an existing audience segment
     */
    AudienceSegment updateSegment(Long id, AudienceSegment segment);

    /**
     * Delete an audience segment
     */
    void deleteSegment(Long id);

    /**
     * Get audience segment by ID
     */
    Optional<AudienceSegment> getSegmentById(Long id);

    /**
     * Get all segments for a company
     */
    List<AudienceSegment> getSegmentsByCompany(Long companyId);

    /**
     * Get active segments for a company
     */
    List<AudienceSegment> getActiveSegmentsByCompany(Long companyId);

    /**
     * Check if a customer matches a segment's filter rules
     */
    boolean customerMatchesSegment(Customer customer, AudienceSegment segment);

    /**
     * Get all customers matching a segment's criteria
     */
    List<Customer> getCustomersInSegment(Long segmentId);

    /**
     * Calculate and update customer count for a segment
     */
    void updateSegmentCustomerCount(Long segmentId);

    /**
     * Recalculate customer counts for all segments that need it
     */
    void recalculateAllSegmentCounts();

    /**
     * Preview customers that would match segment rules (without saving)
     */
    List<Customer> previewSegmentMatch(Long companyId, AudienceSegment segment);

    /**
     * Activate or deactivate a segment
     */
    AudienceSegment toggleSegmentActive(Long id, boolean active);

    /**
     * Check if segment name is unique for company
     */
    boolean isSegmentNameUnique(String name, Long companyId, Long excludeId);
}
