package com.screenleads.backend.app.api.controller;

import com.screenleads.backend.app.domain.model.GeofenceEvent;
import com.screenleads.backend.app.domain.model.GeofenceEventType;
import com.screenleads.backend.app.service.GeofenceEventService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for geofence event management
 */
@RestController
@RequestMapping("/api/geofence/events")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class GeofenceEventController {

    private final GeofenceEventService geofenceEventService;

    /**
     * Record a geofence event manually
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<GeofenceEvent> recordEvent(@Valid @RequestBody GeofenceEvent event) {
        log.info("Recording geofence event: {} for zone: {}", event.getEventType(), event.getZone().getId());
        GeofenceEvent recorded = geofenceEventService.recordEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    /**
     * Process location update from device
     */
    @PostMapping("/location-update")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceEvent>> processLocationUpdate(
            @Valid @RequestBody LocationUpdateRequest request) {
        log.info("Processing location update for device: {}", request.getDeviceId());
        List<GeofenceEvent> events = geofenceEventService.processLocationUpdate(
                request.getDeviceId(),
                request.getLatitude(),
                request.getLongitude()
        );
        return ResponseEntity.ok(events);
    }

    /**
     * Get event by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<GeofenceEvent> getEventById(@PathVariable Long id) {
        return geofenceEventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all events for a zone
     */
    @GetMapping("/zone/{zoneId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceEvent>> getEventsByZone(@PathVariable Long zoneId) {
        log.info("Getting events for zone: {}", zoneId);
        List<GeofenceEvent> events = geofenceEventService.getEventsByZone(zoneId);
        return ResponseEntity.ok(events);
    }

    /**
     * Get all events for a device
     */
    @GetMapping("/device/{deviceId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceEvent>> getEventsByDevice(@PathVariable Long deviceId) {
        log.info("Getting events for device: {}", deviceId);
        List<GeofenceEvent> events = geofenceEventService.getEventsByDevice(deviceId);
        return ResponseEntity.ok(events);
    }

    /**
     * Get recent events for a zone (last 24 hours)
     */
    @GetMapping("/zone/{zoneId}/recent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN') or hasRole('USER')")
    public ResponseEntity<List<GeofenceEvent>> getRecentEventsByZone(@PathVariable Long zoneId) {
        log.info("Getting recent events for zone: {}", zoneId);
        List<GeofenceEvent> events = geofenceEventService.getRecentEventsByZone(zoneId);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events by type within date range
     */
    @GetMapping("/type/{eventType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<List<GeofenceEvent>> getEventsByTypeAndDateRange(
            @PathVariable GeofenceEventType eventType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Getting events by type {} between {} and {}", eventType, startDate, endDate);
        List<GeofenceEvent> events = geofenceEventService.getEventsByTypeAndDateRange(eventType, startDate, endDate);
        return ResponseEntity.ok(events);
    }

    /**
     * Count events by type for a zone
     */
    @GetMapping("/zone/{zoneId}/count/{eventType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Long> countEventsByZoneAndType(
            @PathVariable Long zoneId,
            @PathVariable GeofenceEventType eventType) {
        Long count = geofenceEventService.countEventsByZoneAndType(zoneId, eventType);
        return ResponseEntity.ok(count);
    }

    /**
     * DTO for location update requests
     */
    @Data
    public static class LocationUpdateRequest {
        @NotNull
        private Long deviceId;
        
        @NotNull
        private Double latitude;
        
        @NotNull
        private Double longitude;
    }
}
