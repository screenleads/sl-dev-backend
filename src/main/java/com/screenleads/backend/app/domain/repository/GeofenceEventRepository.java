package com.screenleads.backend.app.domain.repository;

import com.screenleads.backend.app.domain.model.GeofenceEvent;
import com.screenleads.backend.app.domain.model.GeofenceEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GeofenceEventRepository extends JpaRepository<GeofenceEvent, Long> {

    /**
     * Find events for a specific zone
     */
    List<GeofenceEvent> findByZone_IdOrderByCreatedAtDesc(Long zoneId);

    /**
     * Find events for a specific device
     */
    List<GeofenceEvent> findByDevice_IdOrderByCreatedAtDesc(Long deviceId);

    /**
     * Find events by type within date range
     */
    List<GeofenceEvent> findByEventTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            GeofenceEventType eventType, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find recent events for a zone (last 24 hours)
     */
    @Query("SELECT e FROM GeofenceEvent e WHERE e.zone.id = :zoneId " +
            "AND e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<GeofenceEvent> findRecentEventsByZone(@Param("zoneId") Long zoneId, @Param("since") LocalDateTime since);

    /**
     * Count events by type for a zone
     */
    Long countByZone_IdAndEventType(Long zoneId, GeofenceEventType eventType);

    /**
     * Find events for a specific zone and device
     */
    List<GeofenceEvent> findByZone_IdAndDevice_IdOrderByCreatedAtDesc(Long zoneId, Long deviceId);
}