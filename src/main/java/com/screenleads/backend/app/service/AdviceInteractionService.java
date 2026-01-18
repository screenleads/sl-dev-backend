package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.AdviceInteraction;
import com.screenleads.backend.app.domain.model.InteractionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Advice (promotion) interactions for analytics
 */
public interface AdviceInteractionService {

    /**
     * Create a new interaction record
     */
    AdviceInteraction createInteraction(AdviceInteraction interaction);

    /**
     * Get interaction by ID
     */
    Optional<AdviceInteraction> getInteractionById(Long id);

    /**
     * Get all interactions for a specific impression
     */
    List<AdviceInteraction> getInteractionsByImpressionId(Long impressionId);

    /**
     * Get all interactions for a specific customer
     */
    List<AdviceInteraction> getInteractionsByCustomerId(Long customerId);

    /**
     * Get interactions by type
     */
    List<AdviceInteraction> getInteractionsByType(InteractionType type);

    /**
     * Get all interactions for a specific Advice
     */
    List<AdviceInteraction> getInteractionsByAdviceId(Long adviceId);

    /**
     * Get interactions for an Advice within a date range
     */
    List<AdviceInteraction> getInteractionsByAdviceIdAndDateRange(
        Long adviceId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * Count total interactions for an Advice
     */
    Long countInteractionsByAdviceId(Long adviceId);

    /**
     * Count interactions by type for an Advice
     */
    Long countInteractionsByAdviceIdAndType(Long adviceId, InteractionType type);

    /**
     * Count conversion interactions for an Advice
     */
    Long countConversionsByAdviceId(Long adviceId);

    /**
     * Get unique customer count who interacted with an Advice
     */
    Long getUniqueCustomerCountByAdviceId(Long adviceId);

    /**
     * Calculate average interaction duration for an Advice
     */
    Double calculateAverageDurationByAdviceId(Long adviceId);
}
