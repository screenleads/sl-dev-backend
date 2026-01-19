package com.screenleads.backend.app.infrastructure.repository;

import com.screenleads.backend.app.domain.model.FraudAlert;
import com.screenleads.backend.app.domain.model.FraudAlertStatus;
import com.screenleads.backend.app.domain.model.FraudSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    Page<FraudAlert> findByCompany_Id(Long companyId, Pageable pageable);

    Page<FraudAlert> findByCompany_IdAndStatus(Long companyId, FraudAlertStatus status, Pageable pageable);

    Page<FraudAlert> findByCompany_IdAndSeverity(Long companyId, FraudSeverity severity, Pageable pageable);

    List<FraudAlert> findByCompany_IdAndStatusAndSeverityOrderByDetectedAtDesc(
            Long companyId,
            FraudAlertStatus status,
            FraudSeverity severity);

    @Query("SELECT fa FROM FraudAlert fa WHERE fa.company.id = :companyId " +
            "AND fa.status IN :statuses " +
            "ORDER BY fa.detectedAt DESC")
    List<FraudAlert> findAlertsByStatuses(
            @Param("companyId") Long companyId,
            @Param("statuses") List<FraudAlertStatus> statuses);

    long countByCompany_IdAndStatus(Long companyId, FraudAlertStatus status);

    long countByCompany_IdAndSeverity(Long companyId, FraudSeverity severity);

    @Query("SELECT COUNT(fa) FROM FraudAlert fa WHERE fa.company.id = :companyId " +
            "AND fa.detectedAt >= :since")
    long countRecentAlerts(@Param("companyId") Long companyId, @Param("since") LocalDateTime since);
}
