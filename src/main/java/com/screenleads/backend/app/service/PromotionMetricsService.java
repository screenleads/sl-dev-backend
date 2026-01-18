package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.PromotionMetrics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing and calculating Promotion Metrics
 */
public interface PromotionMetricsService {

    /**
     * Calculate and save daily metrics for a specific date
     * This is typically called by a scheduled job
     */
    void calculateDailyMetrics(LocalDate date);

    /**
     * Calculate and save daily metrics for yesterday
     */
    void calculateDailyMetricsForYesterday();

    /**
     * Get metrics for a specific advice and date
     */
    Optional<PromotionMetrics> getMetricsByAdviceAndDate(Long adviceId, LocalDate date);

    /**
     * Get all metrics for a specific advice
     */
    List<PromotionMetrics> getMetricsByAdvice(Long adviceId);

    /**
     * Get metrics for a specific advice within a date range
     */
    List<PromotionMetrics> getMetricsByAdviceAndDateRange(Long adviceId, LocalDate startDate, LocalDate endDate);

    /**
     * Get metrics for all promotions on a specific date
     */
    List<PromotionMetrics> getMetricsByDate(LocalDate date);

    /**
     * Get top performing promotions by conversion rate
     */
    List<PromotionMetrics> getTopPerformingByConversionRate(LocalDate startDate, LocalDate endDate, int limit);

    /**
     * Get top performing promotions by total conversions
     */
    List<PromotionMetrics> getTopPerformingByConversions(LocalDate startDate, LocalDate endDate, int limit);

    /**
     * Get total impressions for a specific advice
     */
    Long getTotalImpressionsByAdvice(Long adviceId);

    /**
     * Get total conversions for a specific advice
     */
    Long getTotalConversionsByAdvice(Long adviceId);

    /**
     * Check if metrics exist for a specific date
     */
    boolean metricsExistForDate(LocalDate date);

    /**
     * Get the latest date for which metrics were calculated
     */
    Optional<LocalDate> getLatestMetricDate();
}
