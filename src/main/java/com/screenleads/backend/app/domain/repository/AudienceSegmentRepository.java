package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.AudienceSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AudienceSegmentRepository extends JpaRepository<AudienceSegment, Long> {

    /**
     * Find all audience segments for a specific company
     */
    List<AudienceSegment> findByCompany_IdOrderByCreatedAtDesc(Long companyId);

    /**
     * Find active audience segments for a company
     */
    List<AudienceSegment> findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(Long companyId);

    /**
     * Find audience segment by name and company
     */
    Optional<AudienceSegment> findByNameAndCompany_Id(String name, Long companyId);

    /**
     * Find segments that need customer count recalculation
     */
    @Query("SELECT a FROM AudienceSegment a WHERE a.isActive = true " +
            "AND (a.lastCalculatedAt IS NULL OR a.lastCalculatedAt < CURRENT_TIMESTAMP - 1 HOUR)")
    List<AudienceSegment> findSegmentsNeedingRecalculation();

    /**
     * Count active segments for a company
     */
    Long countByCompany_IdAndIsActiveTrue(Long companyId);

    /**
     * Check if segment name exists for a company
     */
    boolean existsByNameAndCompany_Id(String name, Long companyId);

    /**
     * Find segments created by a specific user
     */
    List<AudienceSegment> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Get total customer count across all active segments for a company
     */
    @Query("SELECT COALESCE(SUM(a.customerCount), 0) FROM AudienceSegment a " +
            "WHERE a.company.id = :companyId AND a.isActive = true")
    Long getTotalCustomerCountByCompanyId(@Param("companyId") Long companyId);
}
