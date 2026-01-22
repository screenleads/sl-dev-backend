package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repository.GeofenceEventRepository;
import com.screenleads.backend.app.domain.repositories.DeviceRepository;
import com.screenleads.backend.app.service.GeofenceEventService;
import com.screenleads.backend.app.service.GeofenceZoneService;
import com.screenleads.backend.app.service.GeofenceRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceEventServiceImpl implements GeofenceEventService {

    private final GeofenceEventRepository geofenceEventRepository;
    private final GeofenceZoneService geofenceZoneService;
    private final GeofenceRuleService geofenceRuleService;
    private final DeviceRepository deviceRepository;

    @Override
    @Transactional
    public GeofenceEvent recordEvent(GeofenceEvent event) {
        log.info("Recording geofence event: {} for device ID: {} in zone ID: {}",
                event.getEventType(), event.getDevice().getId(), event.getZone().getId());

        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }

        return geofenceEventRepository.save(event);
    }

    @Override
    @Transactional
    public List<GeofenceEvent> processLocationUpdate(Long deviceId,
            double latitude, double longitude) {
        log.debug("Processing location update for device {}: ({}, {})", deviceId, latitude, longitude);

        List<GeofenceEvent> events = new ArrayList<>();

        // Get device
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        // Find zones containing this point
        List<GeofenceZone> zones = geofenceZoneService.findZonesContainingPoint(
                device.getCompany().getId(), latitude, longitude);

        // TODO: Implement entry/exit detection based on previous location
        // For now, just record entry events
        for (GeofenceZone zone : zones) {
            GeofenceEvent event = GeofenceEvent.builder()
                    .device(device)
                    .zone(zone)
                    .eventType(GeofenceEventType.ENTER)
                    .timestamp(LocalDateTime.now())
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();

            events.add(recordEvent(event));

            // Check for applicable rules and trigger promotions if needed
            List<GeofenceRule> rules = geofenceRuleService.findApplicableRules(zone.getId());
            log.debug("Found {} applicable rules for zone {}", rules.size(), zone.getId());
        }

        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GeofenceEvent> getEventById(Long id) {
        return geofenceEventRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceEvent> getEventsByZone(Long zoneId) {
        return geofenceEventRepository.findByZone_IdOrderByCreatedAtDesc(zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceEvent> getEventsByDevice(Long deviceId) {
        return geofenceEventRepository.findByDevice_IdOrderByCreatedAtDesc(deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceEvent> getRecentEventsByZone(Long zoneId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return geofenceEventRepository.findRecentEventsByZone(zoneId, since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceEvent> getEventsByTypeAndDateRange(GeofenceEventType eventType,
            LocalDateTime startDate, LocalDateTime endDate) {
        return geofenceEventRepository.findByEventTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                eventType, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countEventsByZoneAndType(Long zoneId, GeofenceEventType eventType) {
        return geofenceEventRepository.countByZone_IdAndEventType(zoneId, eventType);
    }
}
