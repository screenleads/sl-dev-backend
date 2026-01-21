package com.screenleads.backend.app.api.controller;

import com.screenleads.backend.app.domain.model.GeofenceZone;
import com.screenleads.backend.app.service.GeofenceZoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for geofence zone management
 */
@RestController
@RequestMapping("/api/geofence/zones")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class GeofenceZoneController {

    private final GeofenceZoneService geofenceZoneService;

    /**
     * Create a new geofence zone
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<GeofenceZone> createZone(@Valid @RequestBody GeofenceZone zone) {
        log.info("Creating new geofence zone: {}", zone.getName());
        GeofenceZone created = geofenceZoneService.createZone(zone);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing geofence zone
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<GeofenceZone> updateZone(
            @PathVariable Long id,
            @Valid @RequestBody GeofenceZone zone) {
        log.info("Updating geofence zone: {}", id);
        GeofenceZone updated = geofenceZoneService.updateZone(id, zone);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a geofence zone
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        log.info("Deleting geofence zone: {}", id);
        geofenceZoneService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get geofence zone by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<GeofenceZone> getZoneById(@PathVariable Long id) {
        return geofenceZoneService.getZoneById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all zones for a company
     */
    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceZone>> getZonesByCompany(@PathVariable Long companyId) {
        log.info("Getting all geofence zones for company: {}", companyId);
        List<GeofenceZone> zones = geofenceZoneService.getZonesByCompany(companyId);
        return ResponseEntity.ok(zones);
    }

    /**
     * Get active zones for a company
     */
    @GetMapping("/company/{companyId}/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceZone>> getActiveZonesByCompany(@PathVariable Long companyId) {
        log.info("Getting active geofence zones for company: {}", companyId);
        List<GeofenceZone> zones = geofenceZoneService.getActiveZonesByCompany(companyId);
        return ResponseEntity.ok(zones);
    }

    /**
     * Find zones containing a specific point
     */
    @GetMapping("/company/{companyId}/point")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceZone>> findZonesContainingPoint(
            @PathVariable Long companyId,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        log.info("Finding zones containing point ({}, {}) for company: {}", latitude, longitude, companyId);
        List<GeofenceZone> zones = geofenceZoneService.findZonesContainingPoint(companyId, latitude, longitude);
        return ResponseEntity.ok(zones);
    }

    /**
     * Activate or deactivate a zone
     */
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<GeofenceZone> toggleZoneActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        log.info("Toggling geofence zone {} active status to: {}", id, active);
        GeofenceZone updated = geofenceZoneService.toggleZoneActive(id, active);
        return ResponseEntity.ok(updated);
    }

    /**
     * Count active zones for a company
     */
    @GetMapping("/company/{companyId}/count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Long> countActiveZones(@PathVariable Long companyId) {
        Long count = geofenceZoneService.countActiveZones(companyId);
        return ResponseEntity.ok(count);
    }
}
