package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.GeofenceZone;
import com.screenleads.backend.app.domain.repository.GeofenceZoneRepository;
import com.screenleads.backend.app.service.GeofenceZoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceZoneServiceImpl implements GeofenceZoneService {

    private final GeofenceZoneRepository geofenceZoneRepository;

    @Override
    @Transactional
    public GeofenceZone createZone(GeofenceZone zone) {
        log.info("Creating new geofence zone: {} for company ID: {}", 
            zone.getName(), zone.getCompany().getId());
        return geofenceZoneRepository.save(zone);
    }

    @Override
    @Transactional
    public GeofenceZone updateZone(Long id, GeofenceZone zone) {
        GeofenceZone existing = geofenceZoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Geofence zone not found: " + id));

        log.info("Updating geofence zone ID: {}", id);

        existing.setName(zone.getName());
        existing.setDescription(zone.getDescription());
        existing.setType(zone.getType());
        existing.setGeometry(zone.getGeometry());
        existing.setIsActive(zone.getIsActive());
        existing.setColor(zone.getColor());
        existing.setMetadata(zone.getMetadata());

        return geofenceZoneRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteZone(Long id) {
        log.info("Deleting geofence zone ID: {}", id);
        geofenceZoneRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GeofenceZone> getZoneById(Long id) {
        return geofenceZoneRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceZone> getZonesByCompany(Long companyId) {
        return geofenceZoneRepository.findByCompany_IdOrderByCreatedAtDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceZone> getActiveZonesByCompany(Long companyId) {
        return geofenceZoneRepository.findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceZone> findZonesContainingPoint(Long companyId, double latitude, double longitude) {
        log.debug("Finding zones containing point ({}, {}) for company {}", latitude, longitude, companyId);
        
        List<GeofenceZone> activeZones = getActiveZonesByCompany(companyId);
        
        return activeZones.stream()
                .filter(zone -> zone.containsPoint(latitude, longitude))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GeofenceZone toggleZoneActive(Long id, boolean active) {
        GeofenceZone zone = geofenceZoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Geofence zone not found: " + id));

        log.info("Toggling geofence zone {} active status to: {}", id, active);
        zone.setIsActive(active);
        return geofenceZoneRepository.save(zone);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isZoneNameUnique(String name, Long companyId, Long excludeId) {
        Optional<GeofenceZone> existing = geofenceZoneRepository.findByNameAndCompany_Id(name, companyId);
        return !existing.isPresent() || (excludeId != null && existing.get().getId().equals(excludeId));
    }

    @Override
    @Transactional(readOnly = true)
    public Long countActiveZones(Long companyId) {
        return geofenceZoneRepository.countByCompany_IdAndIsActiveTrue(companyId);
    }
}
