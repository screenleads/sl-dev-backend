package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.AdviceImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdviceImpressionRepository extends JpaRepository<AdviceImpression, Long> {

    /**
     * Find all impressions for a specific Advice
     */
    List<AdviceImpression> findByAdvice_Id(Long adviceId);

    /**
     * Find all impressions for a specific Device
     */
    List<AdviceImpression> findByDevice_Id(Long deviceId);

    /**
     * Find all impressions for a specific Customer
     */
    List<AdviceImpression> findByCustomer_Id(Long customerId);

    /**
     * Find impressions by session ID
     */
    List<AdviceImpression> findBySessionId(String sessionId);

    /**
     * Find impressions within a date range for a specific Advice
     */
    @Query("SELECT ai FROM AdviceImpression ai WHERE ai.advice.id = :adviceId " +
            "AND ai.timestamp BETWEEN :startDate AND :endDate")
    List<AdviceImpression> findByAdviceIdAndDateRange(
            @Param("adviceId") Long adviceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count total impressions for an Advice
     */
    Long countByAdvice_Id(Long adviceId);

    /**
     * Count interactive impressions for an Advice
     */
    Long countByAdvice_IdAndWasInteractiveTrue(Long adviceId);

    /**
     * Find unique customers who viewed a specific Advice
     */
    @Query("SELECT DISTINCT ai.customer.id FROM AdviceImpression ai " +
            "WHERE ai.advice.id = :adviceId AND ai.customer IS NOT NULL")
    List<Long> findUniqueCustomerIdsByAdviceId(@Param("adviceId") Long adviceId);

    /**
     * Calculate average duration for an Advice
     */
    @Query("SELECT AVG(ai.durationSeconds) FROM AdviceImpression ai " +
            "WHERE ai.advice.id = :adviceId AND ai.durationSeconds IS NOT NULL")
    Double calculateAverageDurationByAdviceId(@Param("adviceId") Long adviceId);

    /**
     * Count impressions for an Advice within a date range
     */
    @Query("SELECT COUNT(ai) FROM AdviceImpression ai WHERE ai.advice.id = :adviceId " +
            "AND ai.timestamp BETWEEN :startDate AND :endDate")
    Long countByAdviceIdAndDateRange(
            @Param("adviceId") Long adviceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count unique customers for an Advice within a date range
     */
    @Query("SELECT COUNT(DISTINCT ai.customer.id) FROM AdviceImpression ai " +
            "WHERE ai.advice.id = :adviceId AND ai.customer IS NOT NULL " +
            "AND ai.timestamp BETWEEN :startDate AND :endDate")
    Long countUniqueCustomersByAdviceIdAndDateRange(
            @Param("adviceId") Long adviceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count unique devices for an Advice within a date range
     */
    @Query("SELECT COUNT(DISTINCT ai.device.id) FROM AdviceImpression ai " +
            "WHERE ai.advice.id = :adviceId AND ai.device IS NOT NULL " +
            "AND ai.timestamp BETWEEN :startDate AND :endDate")
    Long countUniqueDevicesByAdviceIdAndDateRange(
            @Param("adviceId") Long adviceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate average duration for an Advice within a date range
     */
    @Query("SELECT AVG(ai.durationSeconds) FROM AdviceImpression ai " +
            "WHERE ai.advice.id = :adviceId AND ai.durationSeconds IS NOT NULL " +
            "AND ai.timestamp BETWEEN :startDate AND :endDate")
    Double calculateAverageDurationByAdviceIdAndDateRange(
            @Param("adviceId") Long adviceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
