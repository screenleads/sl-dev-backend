package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.AdviceImpression;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Advice (promotion) impressions for analytics
 */
public interface AdviceImpressionService {

    /**
     * Create a new impression record
     */
    AdviceImpression createImpression(AdviceImpression impression);

    /**
     * Get impression by ID
     */
    Optional<AdviceImpression> getImpressionById(Long id);

    /**
     * Get all impressions for a specific Advice
     */
    List<AdviceImpression> getImpressionsByAdviceId(Long adviceId);

    /**
     * Get all impressions for a specific Device
     */
    List<AdviceImpression> getImpressionsByDeviceId(Long deviceId);

    /**
     * Get all impressions for a specific Customer
     */
    List<AdviceImpression> getImpressionsByCustomerId(Long customerId);

    /**
     * Get impressions by session ID
     */
    List<AdviceImpression> getImpressionsBySessionId(String sessionId);

    /**
     * Get impressions for an Advice within a date range
     */
    List<AdviceImpression> getImpressionsByAdviceIdAndDateRange(
        Long adviceId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * Count total impressions for an Advice
     */
    Long countImpressionsByAdviceId(Long adviceId);

    /**
     * Count interactive impressions for an Advice
     */
    Long countInteractiveImpressionsByAdviceId(Long adviceId);

    /**
     * Get unique customer count for an Advice
     */
    Long getUniqueCustomerCountByAdviceId(Long adviceId);

    /**
     * Calculate average view duration for an Advice
     */
    Double calculateAverageDurationByAdviceId(Long adviceId);
}
