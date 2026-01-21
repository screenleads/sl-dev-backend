package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.GeofenceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeofenceZoneRepository extends JpaRepository<GeofenceZone, Long> {

    /**
     * Find all geofence zones for a specific company
     */
    List<GeofenceZone> findByCompany_IdOrderByCreatedAtDesc(Long companyId);

    /**
     * Find active geofence zones for a company
     */
    List<GeofenceZone> findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(Long companyId);

    /**
     * Find geofence zone by name and company
     */
    Optional<GeofenceZone> findByNameAndCompany_Id(String name, Long companyId);

    /**
     * Count active zones for a company
     */
    Long countByCompany_IdAndIsActiveTrue(Long companyId);

    /**
     * Check if zone name exists for a company
     */
    boolean existsByNameAndCompany_Id(String name, Long companyId);

    /**
     * Find zones created by a specific user
     */
    List<GeofenceZone> findByCreatedBy_IdOrderByCreatedAtDesc(Long userId);

    /**
     * Get all active zones (for background processing)
     */
    List<GeofenceZone> findByIsActiveTrue();
}
