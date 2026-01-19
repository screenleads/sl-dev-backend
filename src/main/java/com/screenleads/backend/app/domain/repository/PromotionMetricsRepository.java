package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.PromotionMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionMetricsRepository extends JpaRepository<PromotionMetrics, Long> {

    /**
     * Find metrics for a specific advice and date
     */
    Optional<PromotionMetrics> findByAdvice_IdAndMetricDate(Long adviceId, LocalDate metricDate);

    /**
     * Get all metrics for a specific advice
     */
    List<PromotionMetrics> findByAdvice_IdOrderByMetricDateDesc(Long adviceId);

    /**
     * Get metrics for a specific advice within a date range
     */
    @Query("SELECT pm FROM PromotionMetrics pm WHERE pm.advice.id = :adviceId " +
            "AND pm.metricDate BETWEEN :startDate AND :endDate ORDER BY pm.metricDate DESC")
    List<PromotionMetrics> findByAdviceIdAndDateRange(
            @Param("adviceId") Long adviceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get metrics for all promotions on a specific date
     */
    List<PromotionMetrics> findByMetricDateOrderByTotalImpressionsDesc(LocalDate metricDate);

    /**
     * Get top performing promotions by conversion rate within a date range
     */
    @Query("SELECT pm FROM PromotionMetrics pm WHERE pm.metricDate BETWEEN :startDate AND :endDate " +
            "ORDER BY pm.conversionRate DESC, pm.totalConversions DESC")
    List<PromotionMetrics> findTopPerformingByConversionRate(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get top performing promotions by total conversions within a date range
     */
    @Query("SELECT pm FROM PromotionMetrics pm WHERE pm.metricDate BETWEEN :startDate AND :endDate " +
            "ORDER BY pm.totalConversions DESC, pm.conversionRate DESC")
    List<PromotionMetrics> findTopPerformingByConversions(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get total impressions for a specific advice across all dates
     */
    @Query("SELECT COALESCE(SUM(pm.totalImpressions), 0) FROM PromotionMetrics pm WHERE pm.advice.id = :adviceId")
    Long getTotalImpressionsByAdviceId(@Param("adviceId") Long adviceId);

    /**
     * Get total conversions for a specific advice across all dates
     */
    @Query("SELECT COALESCE(SUM(pm.totalConversions), 0) FROM PromotionMetrics pm WHERE pm.advice.id = :adviceId")
    Long getTotalConversionsByAdviceId(@Param("adviceId") Long adviceId);

    /**
     * Check if metrics exist for a specific date
     */
    boolean existsByMetricDate(LocalDate metricDate);

    /**
     * Delete metrics older than a specific date (for cleanup)
     */
    void deleteByMetricDateBefore(LocalDate date);

    /**
     * Get latest metrics date
     */
    @Query("SELECT MAX(pm.metricDate) FROM PromotionMetrics pm")
    Optional<LocalDate> findLatestMetricDate();
}
