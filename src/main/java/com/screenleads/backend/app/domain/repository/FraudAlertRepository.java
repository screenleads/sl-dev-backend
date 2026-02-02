package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.FraudAlert;
import com.screenleads.backend.app.domain.model.FraudAlertStatus;
import com.screenleads.backend.app.domain.model.FraudSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for FraudAlert entity
 */
@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

        /**
         * Find all fraud alerts by company ID
         */
        List<FraudAlert> findByCompany_Id(Long companyId);

        /**
         * Find fraud alerts by rule ID
         */
        List<FraudAlert> findByRule_Id(Long ruleId);

        /**
         * Find fraud alerts by status
         */
        List<FraudAlert> findByCompany_IdAndStatus(Long companyId, FraudAlertStatus status);

        /**
         * Find fraud alerts by severity
         */
        List<FraudAlert> findByCompany_IdAndSeverity(Long companyId, FraudSeverity severity);

        /**
         * Find pending alerts (PENDING or INVESTIGATING)
         */
        @Query("SELECT a FROM FraudAlert a WHERE a.company.id = :companyId " +
                        "AND a.status IN ('PENDING', 'INVESTIGATING') " +
                        "ORDER BY a.severity DESC, a.detectedAt DESC")
        List<FraudAlert> findPendingAlertsByCompany(@Param("companyId") Long companyId);

        /**
         * Find recent alerts (last N days)
         */
        @Query("SELECT a FROM FraudAlert a WHERE a.company.id = :companyId " +
                        "AND a.detectedAt >= :since " +
                        "ORDER BY a.detectedAt DESC")
        List<FraudAlert> findRecentAlertsByCompany(
                        @Param("companyId") Long companyId,
                        @Param("since") LocalDateTime since);

        /**
         * Find alerts by related entity
         */
        List<FraudAlert> findByCompany_IdAndRelatedEntityTypeAndRelatedEntityId(
                        Long companyId,
                        String relatedEntityType,
                        Long relatedEntityId);

        /**
         * Count alerts by status
         */
        long countByCompany_IdAndStatus(Long companyId, FraudAlertStatus status);

        /**
         * Count alerts by severity
         */
        long countByCompany_IdAndSeverity(Long companyId, FraudSeverity severity);

        /**
         * Find high priority alerts (HIGH or CRITICAL severity, PENDING or
         * INVESTIGATING status)
         */
        @Query("SELECT a FROM FraudAlert a WHERE a.company.id = :companyId " +
                        "AND a.severity IN ('HIGH', 'CRITICAL') " +
                        "AND a.status IN ('PENDING', 'INVESTIGATING') " +
                        "ORDER BY a.severity DESC, a.detectedAt DESC")
        List<FraudAlert> findHighPriorityAlertsByCompany(@Param("companyId") Long companyId);

        /**
         * Find alerts by multiple statuses
         */
        @Query("SELECT a FROM FraudAlert a WHERE a.company.id = :companyId " +
                        "AND a.status IN :statuses " +
                        "ORDER BY a.detectedAt DESC")
        List<FraudAlert> findAlertsByStatuses(
                        @Param("companyId") Long companyId,
                        @Param("statuses") List<FraudAlertStatus> statuses);

        /**
         * Count recent alerts
         */
        @Query("SELECT COUNT(a) FROM FraudAlert a WHERE a.company.id = :companyId " +
                        "AND a.detectedAt >= :since")
        long countRecentAlerts(
                        @Param("companyId") Long companyId,
                        @Param("since") LocalDateTime since);
}
