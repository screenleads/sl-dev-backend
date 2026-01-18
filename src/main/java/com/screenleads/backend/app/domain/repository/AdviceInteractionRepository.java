package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.AdviceInteraction;
import com.screenleads.backend.app.domain.model.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdviceInteractionRepository extends JpaRepository<AdviceInteraction, Long> {

    /**
     * Find all interactions for a specific impression
     */
    List<AdviceInteraction> findByImpression_Id(Long impressionId);

    /**
     * Find all interactions for a specific customer
     */
    List<AdviceInteraction> findByCustomer_Id(Long customerId);

    /**
     * Find interactions by type
     */
    List<AdviceInteraction> findByType(InteractionType type);

    /**
     * Find all interactions for a specific Advice (through impression)
     */
    @Query("SELECT ai FROM AdviceInteraction ai WHERE ai.impression.advice.id = :adviceId")
    List<AdviceInteraction> findByAdviceId(@Param("adviceId") Long adviceId);

    /**
     * Find interactions for an Advice within a date range
     */
    @Query("SELECT ai FROM AdviceInteraction ai " +
           "WHERE ai.impression.advice.id = :adviceId " +
           "AND ai.timestamp BETWEEN :startDate AND :endDate")
    List<AdviceInteraction> findByAdviceIdAndDateRange(
        @Param("adviceId") Long adviceId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count total interactions for an Advice
     */
    @Query("SELECT COUNT(ai) FROM AdviceInteraction ai WHERE ai.impression.advice.id = :adviceId")
    Long countByAdviceId(@Param("adviceId") Long adviceId);

    /**
     * Count interactions by type for an Advice
     */
    @Query("SELECT COUNT(ai) FROM AdviceInteraction ai " +
           "WHERE ai.impression.advice.id = :adviceId AND ai.type = :type")
    Long countByAdviceIdAndType(
        @Param("adviceId") Long adviceId,
        @Param("type") InteractionType type
    );

    /**
     * Count conversion interactions for an Advice
     */
    @Query("SELECT COUNT(ai) FROM AdviceInteraction ai " +
           "WHERE ai.impression.advice.id = :adviceId AND ai.isConversion = true")
    Long countConversionsByAdviceId(@Param("adviceId") Long adviceId);

    /**
     * Get unique customers who interacted with an Advice
     */
    @Query("SELECT DISTINCT ai.customer.id FROM AdviceInteraction ai " +
           "WHERE ai.impression.advice.id = :adviceId AND ai.customer IS NOT NULL")
    List<Long> findUniqueCustomerIdsByAdviceId(@Param("adviceId") Long adviceId);

    /**
     * Calculate average interaction duration for an Advice
     */
    @Query("SELECT AVG(ai.durationSeconds) FROM AdviceInteraction ai " +
           "WHERE ai.impression.advice.id = :adviceId AND ai.durationSeconds IS NOT NULL")
    Double calculateAverageDurationByAdviceId(@Param("adviceId") Long adviceId);
    /**
     * Count interactions for an Advice within a date range
     */
    @Query("SELECT COUNT(ai) FROM AdviceInteraction ai " +
           "WHERE ai.impression.advice.id = :adviceId " +
           "AND ai.timestamp BETWEEN :startDate AND :endDate")
    Long countByAdviceIdAndDateRange(
        @Param("adviceId") Long adviceId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count conversions for an Advice within a date range
     */
    @Query("SELECT COUNT(ai) FROM AdviceInteraction ai " +
           "WHERE ai.impression.advice.id = :adviceId AND ai.isConversion = true " +
           "AND ai.timestamp BETWEEN :startDate AND :endDate")
    Long countConversionsByAdviceIdAndDateRange(
        @Param("adviceId") Long adviceId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );}
