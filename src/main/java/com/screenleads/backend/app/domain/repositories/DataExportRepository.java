package com.screenleads.backend.app.domain.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.screenleads.backend.app.domain.model.DataExport;
import com.screenleads.backend.app.domain.model.ExportStatus;
import com.screenleads.backend.app.domain.model.ExportType;

public interface DataExportRepository extends JpaRepository<DataExport, Long> {

    List<DataExport> findByCompanyId(Long companyId);

    List<DataExport> findByRequestedById(Long userId);

    List<DataExport> findByStatus(ExportStatus status);

    List<DataExport> findByExportType(ExportType exportType);

    @Query("SELECT de FROM DataExport de WHERE de.company.id = :companyId ORDER BY de.createdAt DESC")
    List<DataExport> findByCompanyIdOrderByCreatedAtDesc(@Param("companyId") Long companyId);

    @Query("SELECT de FROM DataExport de WHERE de.status = :status AND de.expiresAt < :now")
    List<DataExport> findExpiredExports(@Param("status") ExportStatus status, @Param("now") Instant now);

    @Query("SELECT de FROM DataExport de WHERE de.company.id = :companyId AND de.status = 'COMPLETED' AND de.expiresAt > :now")
    List<DataExport> findAvailableExportsByCompany(@Param("companyId") Long companyId, @Param("now") Instant now);
}
