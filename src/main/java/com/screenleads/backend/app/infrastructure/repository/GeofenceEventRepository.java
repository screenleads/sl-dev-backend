package com.screenleads.backend.app.infrastructure.repository;

import com.screenleads.backend.app.domain.model.GeofenceEvent;
import com.screenleads.backend.app.domain.model.GeofenceEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GeofenceEventRepository extends JpaRepository<GeofenceEvent, Long> {

    Page<GeofenceEvent> findByDevice_Id(Long deviceId, Pageable pageable);

    Page<GeofenceEvent> findByZone_Id(Long zoneId, Pageable pageable);

    @Query("SELECT ge FROM GeofenceEvent ge " +
           "WHERE ge.device.id = :deviceId " +
           "AND ge.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY ge.timestamp DESC")
    List<GeofenceEvent> findDeviceEventsBetween(
        @Param("deviceId") Long deviceId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT ge FROM GeofenceEvent ge " +
           "WHERE ge.zone.id = :zoneId " +
           "AND ge.eventType = :eventType " +
           "AND ge.timestamp >= :since " +
           "ORDER BY ge.timestamp DESC")
    List<GeofenceEvent> findZoneEventsByType(
        @Param("zoneId") Long zoneId,
        @Param("eventType") GeofenceEventType eventType,
        @Param("since") LocalDateTime since
    );

    long countByZone_IdAndEventTypeAndTimestampAfter(
        Long zoneId, 
        GeofenceEventType eventType, 
        LocalDateTime timestamp
    );
}
