package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.GeofenceEvent;
import com.screenleads.backend.app.domain.model.GeofenceRule;
import com.screenleads.backend.app.domain.model.GeofenceZone;
import com.screenleads.backend.app.domain.model.Promotion;

import java.util.List;
import java.util.Map;

public interface GeofenceService {

    // Zone management
    GeofenceZone createZone(GeofenceZone zone);

    GeofenceZone updateZone(Long zoneId, GeofenceZone updatedZone);

    void deleteZone(Long zoneId);

    GeofenceZone getZone(Long zoneId);

    List<GeofenceZone> getZonesByCompany(Long companyId);

    List<GeofenceZone> getActiveZonesByCompany(Long companyId);

    // Rule management
    GeofenceRule createRule(GeofenceRule rule);

    GeofenceRule updateRule(Long ruleId, GeofenceRule updatedRule);

    void deleteRule(Long ruleId);

    List<GeofenceRule> getRulesByPromotion(Long promotionId);

    List<GeofenceRule> getRulesByZone(Long zoneId);

    List<GeofenceRule> getActiveRulesByCompany(Long companyId);

    // Geofence checking
    List<Promotion> checkGeofenceRules(Long deviceId, double latitude, double longitude);

    List<GeofenceZone> findZonesContainingPoint(Long companyId, double latitude, double longitude);

    boolean isInsideAnyZone(Long companyId, double latitude, double longitude);

    // Event tracking
    GeofenceEvent trackEvent(GeofenceEvent event);

    List<GeofenceEvent> getDeviceEvents(Long deviceId, int page, int size);

    List<GeofenceEvent> getZoneEvents(Long zoneId, int page, int size);

    // Analytics
    Map<String, Object> getZoneStatistics(Long zoneId);

    Map<String, Object> getCompanyGeofenceStats(Long companyId);
}
