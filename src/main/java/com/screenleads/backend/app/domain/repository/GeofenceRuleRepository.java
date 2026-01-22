package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.GeofenceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeofenceRuleRepository extends JpaRepository<GeofenceRule, Long> {

    /**
     * Find all rules for a specific company (via zone)
     */
    @Query("SELECT r FROM GeofenceRule r WHERE r.zone.company.id = :companyId " +
            "ORDER BY r.priority DESC, r.createdAt DESC")
    List<GeofenceRule> findByCompany_IdOrderByPriorityDescCreatedAtDesc(@Param("companyId") Long companyId);

    /**
     * Find active rules for a company (via zone)
     */
    @Query("SELECT r FROM GeofenceRule r WHERE r.zone.company.id = :companyId " +
            "AND r.isActive = true ORDER BY r.priority DESC, r.createdAt DESC")
    List<GeofenceRule> findByCompany_IdAndIsActiveTrueOrderByPriorityDescCreatedAtDesc(
            @Param("companyId") Long companyId);

    /**
     * Find rules for a specific zone
     */
    List<GeofenceRule> findByZone_IdOrderByPriorityDesc(Long zoneId);

    /**
     * Find active rules for a specific zone
     */
    List<GeofenceRule> findByZone_IdAndIsActiveTrueOrderByPriorityDesc(Long zoneId);

    /**
     * Find rules by promotion
     */
    List<GeofenceRule> findByPromotion_Id(Long promotionId);

    /**
     * Count active rules for a company (via zone)
     */
    @Query("SELECT COUNT(r) FROM GeofenceRule r WHERE r.zone.company.id = :companyId " +
            "AND r.isActive = true")
    Long countByCompany_IdAndIsActiveTrue(@Param("companyId") Long companyId);

    /**
     * Get all active rules (for event processing)
     */
    List<GeofenceRule> findByIsActiveTrueOrderByPriorityDesc();
}
