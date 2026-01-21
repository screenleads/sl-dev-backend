package com.screenleads.backend.app.service;

import com.screenleads.backend.app.domain.model.GeofenceEvent;
import com.screenleads.backend.app.domain.model.GeofenceEventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing geofence events
 */
public interface GeofenceEventService {

    /**
     * Record a new geofence event (entry/exit)
     */
    GeofenceEvent recordEvent(GeofenceEvent event);

    /**
     * Process a location update and detect geofence events
     */
    List<GeofenceEvent> processLocationUpdate(Long deviceId, double latitude, double longitude);

    /**
     * Get geofence event by ID
     */
    Optional<GeofenceEvent> getEventById(Long id);

    /**
     * Get events for a specific zone
     */
    List<GeofenceEvent> getEventsByZone(Long zoneId);

    /**
     * Get events for a specific device
     */
    List<GeofenceEvent> getEventsByDevice(Long deviceId);

    /**
     * Get recent events for a zone (last 24 hours)
     */
    List<GeofenceEvent> getRecentEventsByZone(Long zoneId);

    /**
     * Get events by type within date range
     */
    List<GeofenceEvent> getEventsByTypeAndDateRange(GeofenceEventType eventType, LocalDateTime startDate,
            LocalDateTime endDate);

    /**
     * Count events by type for a zone
     */
    Long countEventsByZoneAndType(Long zoneId, GeofenceEventType eventType);
}
