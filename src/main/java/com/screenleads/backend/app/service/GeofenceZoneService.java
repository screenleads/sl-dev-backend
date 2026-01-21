package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.GeofenceZone;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing geofence zones
 */
public interface GeofenceZoneService {

    /**
     * Create a new geofence zone
     */
    GeofenceZone createZone(GeofenceZone zone);

    /**
     * Update an existing geofence zone
     */
    GeofenceZone updateZone(Long id, GeofenceZone zone);

    /**
     * Delete a geofence zone
     */
    void deleteZone(Long id);

    /**
     * Get geofence zone by ID
     */
    Optional<GeofenceZone> getZoneById(Long id);

    /**
     * Get all zones for a company
     */
    List<GeofenceZone> getZonesByCompany(Long companyId);

    /**
     * Get active zones for a company
     */
    List<GeofenceZone> getActiveZonesByCompany(Long companyId);

    /**
     * Check if a point is inside any zone for a company
     */
    List<GeofenceZone> findZonesContainingPoint(Long companyId, double latitude, double longitude);

    /**
     * Activate or deactivate a zone
     */
    GeofenceZone toggleZoneActive(Long id, boolean active);

    /**
     * Check if zone name is unique for company
     */
    boolean isZoneNameUnique(String name, Long companyId, Long excludeId);

    /**
     * Count active zones for a company
     */
    Long countActiveZones(Long companyId);
}
