package com.screenleads.backend.app.infrastructure.repository;

import com.screenleads.backend.app.domain.model.GeofenceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeofenceZoneRepository extends JpaRepository<GeofenceZone, Long> {

    List<GeofenceZone> findByCompany_IdAndIsActiveTrue(Long companyId);

    List<GeofenceZone> findByCompany_Id(Long companyId);

    @Query("SELECT gz FROM GeofenceZone gz WHERE gz.company.id = :companyId AND gz.isActive = true")
    List<GeofenceZone> findActiveZonesByCompany(@Param("companyId") Long companyId);

    long countByCompany_Id(Long companyId);

    long countByCompany_IdAndIsActiveTrue(Long companyId);
}
