package com.screenleads.backend.app.application.controller;

import com.screenleads.backend.app.application.service.GeofenceService;
import com.screenleads.backend.app.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/geofence")
@RequiredArgsConstructor
@Slf4j
public class GeofenceController {

    private final GeofenceService geofenceService;

    // ========== Zone Management ==========

    @PostMapping("/zones")
    public ResponseEntity<GeofenceZone> createZone(@RequestBody GeofenceZone zone) {
        log.info("POST /api/geofence/zones - Creating zone: {}", zone.getName());
        GeofenceZone created = geofenceService.createZone(zone);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/zones/{zoneId}")
    public ResponseEntity<GeofenceZone> updateZone(
            @PathVariable Long zoneId,
            @RequestBody GeofenceZone zone) {
        log.info("PUT /api/geofence/zones/{} - Updating zone", zoneId);
        GeofenceZone updated = geofenceService.updateZone(zoneId, zone);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/zones/{zoneId}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long zoneId) {
        log.info("DELETE /api/geofence/zones/{}", zoneId);
        geofenceService.deleteZone(zoneId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/zones/{zoneId}")
    public ResponseEntity<GeofenceZone> getZone(@PathVariable Long zoneId) {
        GeofenceZone zone = geofenceService.getZone(zoneId);
        return ResponseEntity.ok(zone);
    }

    @GetMapping("/zones/company/{companyId}")
    public ResponseEntity<List<GeofenceZone>> getCompanyZones(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<GeofenceZone> zones = activeOnly 
            ? geofenceService.getActiveZonesByCompany(companyId)
            : geofenceService.getZonesByCompany(companyId);
        return ResponseEntity.ok(zones);
    }

    // ========== Rule Management ==========

    @PostMapping("/rules")
    public ResponseEntity<GeofenceRule> createRule(@RequestBody GeofenceRule rule) {
        log.info("POST /api/geofence/rules - Creating rule for promotion: {}", 
            rule.getPromotion().getId());
        GeofenceRule created = geofenceService.createRule(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<GeofenceRule> updateRule(
            @PathVariable Long ruleId,
            @RequestBody GeofenceRule rule) {
        log.info("PUT /api/geofence/rules/{}", ruleId);
        GeofenceRule updated = geofenceService.updateRule(ruleId, rule);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long ruleId) {
        log.info("DELETE /api/geofence/rules/{}", ruleId);
        geofenceService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rules/promotion/{promotionId}")
    public ResponseEntity<List<GeofenceRule>> getPromotionRules(@PathVariable Long promotionId) {
        List<GeofenceRule> rules = geofenceService.getRulesByPromotion(promotionId);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/rules/zone/{zoneId}")
    public ResponseEntity<List<GeofenceRule>> getZoneRules(@PathVariable Long zoneId) {
        List<GeofenceRule> rules = geofenceService.getRulesByZone(zoneId);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/rules/company/{companyId}")
    public ResponseEntity<List<GeofenceRule>> getCompanyRules(@PathVariable Long companyId) {
        List<GeofenceRule> rules = geofenceService.getActiveRulesByCompany(companyId);
        return ResponseEntity.ok(rules);
    }

    // ========== Geofence Checking ==========

    @PostMapping("/check")
    public ResponseEntity<List<Promotion>> checkGeofence(@RequestBody GeofenceCheckRequest request) {
        log.info("POST /api/geofence/check - Device: {}, Lat: {}, Lon: {}", 
            request.getDeviceId(), request.getLatitude(), request.getLongitude());
        
        List<Promotion> allowedPromotions = geofenceService.checkGeofenceRules(
            request.getDeviceId(),
            request.getLatitude(),
            request.getLongitude()
        );
        
        return ResponseEntity.ok(allowedPromotions);
    }

    @GetMapping("/zones/containing")
    public ResponseEntity<List<GeofenceZone>> findZonesContainingPoint(
            @RequestParam Long companyId,
            @RequestParam double latitude,
            @RequestParam double longitude) {
        List<GeofenceZone> zones = geofenceService.findZonesContainingPoint(
            companyId, latitude, longitude
        );
        return ResponseEntity.ok(zones);
    }

    @GetMapping("/check/inside")
    public ResponseEntity<Map<String, Boolean>> checkIfInside(
            @RequestParam Long companyId,
            @RequestParam double latitude,
            @RequestParam double longitude) {
        boolean inside = geofenceService.isInsideAnyZone(companyId, latitude, longitude);
        return ResponseEntity.ok(Map.of("insideAnyZone", inside));
    }

    // ========== Event Tracking ==========

    @PostMapping("/events")
    public ResponseEntity<GeofenceEvent> trackEvent(@RequestBody GeofenceEvent event) {
        log.info("POST /api/geofence/events - Type: {}, Device: {}", 
            event.getEventType(), event.getDevice().getId());
        GeofenceEvent tracked = geofenceService.trackEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(tracked);
    }

    @GetMapping("/events/device/{deviceId}")
    public ResponseEntity<List<GeofenceEvent>> getDeviceEvents(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<GeofenceEvent> events = geofenceService.getDeviceEvents(deviceId, page, size);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/zone/{zoneId}")
    public ResponseEntity<List<GeofenceEvent>> getZoneEvents(
            @PathVariable Long zoneId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<GeofenceEvent> events = geofenceService.getZoneEvents(zoneId, page, size);
        return ResponseEntity.ok(events);
    }

    // ========== Analytics ==========

    @GetMapping("/stats/zone/{zoneId}")
    public ResponseEntity<Map<String, Object>> getZoneStats(@PathVariable Long zoneId) {
        Map<String, Object> stats = geofenceService.getZoneStatistics(zoneId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/company/{companyId}")
    public ResponseEntity<Map<String, Object>> getCompanyStats(@PathVariable Long companyId) {
        Map<String, Object> stats = geofenceService.getCompanyGeofenceStats(companyId);
        return ResponseEntity.ok(stats);
    }

    // ========== DTOs ==========

    public static class GeofenceCheckRequest {
        private Long deviceId;
        private double latitude;
        private double longitude;

        public Long getDeviceId() { return deviceId; }
        public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
        
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
    }
}
