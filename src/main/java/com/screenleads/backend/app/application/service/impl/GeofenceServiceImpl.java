package com.screenleads.backend.app.application.service.impl;

import com.screenleads.backend.app.application.service.GeofenceService;
import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GeofenceServiceImpl implements GeofenceService {

    private final GeofenceZoneRepository zoneRepository;
    private final GeofenceRuleRepository ruleRepository;
    private final GeofenceEventRepository eventRepository;

    @Override
    public GeofenceZone createZone(GeofenceZone zone) {
        log.info("Creating geofence zone: {}", zone.getName());
        zone.setCreatedAt(LocalDateTime.now());
        zone.setUpdatedAt(LocalDateTime.now());
        return zoneRepository.save(zone);
    }

    @Override
    public GeofenceZone updateZone(Long zoneId, GeofenceZone updatedZone) {
        GeofenceZone existing = getZone(zoneId);

        existing.setName(updatedZone.getName());
        existing.setDescription(updatedZone.getDescription());
        existing.setType(updatedZone.getType());
        existing.setGeometry(updatedZone.getGeometry());
        existing.setIsActive(updatedZone.getIsActive());
        existing.setColor(updatedZone.getColor());
        existing.setMetadata(updatedZone.getMetadata());
        existing.setUpdatedAt(LocalDateTime.now());

        return zoneRepository.save(existing);
    }

    @Override
    public void deleteZone(Long zoneId) {
        log.info("Deleting geofence zone: {}", zoneId);
        zoneRepository.deleteById(zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public GeofenceZone getZone(Long zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Zone not found: " + zoneId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceZone> getZonesByCompany(Long companyId) {
        return zoneRepository.findByCompany_Id(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceZone> getActiveZonesByCompany(Long companyId) {
        return zoneRepository.findByCompany_IdAndIsActiveTrue(companyId);
    }

    @Override
    public GeofenceRule createRule(GeofenceRule rule) {
        log.info("Creating geofence rule for promotion: {}", rule.getPromotion().getId());
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        return ruleRepository.save(rule);
    }

    @Override
    public GeofenceRule updateRule(Long ruleId, GeofenceRule updatedRule) {
        GeofenceRule existing = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));

        existing.setRuleType(updatedRule.getRuleType());
        existing.setPriority(updatedRule.getPriority());
        existing.setIsActive(updatedRule.getIsActive());
        existing.setUpdatedAt(LocalDateTime.now());

        return ruleRepository.save(existing);
    }

    @Override
    public void deleteRule(Long ruleId) {
        log.info("Deleting geofence rule: {}", ruleId);
        ruleRepository.deleteById(ruleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceRule> getRulesByPromotion(Long promotionId) {
        return ruleRepository.findByPromotion_Id(promotionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceRule> getRulesByZone(Long zoneId) {
        return ruleRepository.findByZone_Id(zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceRule> getActiveRulesByCompany(Long companyId) {
        return ruleRepository.findActiveRulesByCompany(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> checkGeofenceRules(Long deviceId, double latitude, double longitude) {
        // Obtener todas las reglas activas (todas las compañías por ahora)
        // En producción, debería filtrarse por la compañía del device
        List<GeofenceRule> rules = ruleRepository.findByIsActiveTrue();

        // Filtrar promociones según las reglas de geofencing
        return rules.stream()
                .filter(rule -> rule.allowsPromotion(latitude, longitude))
                .map(GeofenceRule::getPromotion)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceZone> findZonesContainingPoint(Long companyId, double latitude, double longitude) {
        List<GeofenceZone> zones = getActiveZonesByCompany(companyId);

        return zones.stream()
                .filter(zone -> zone.containsPoint(latitude, longitude))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInsideAnyZone(Long companyId, double latitude, double longitude) {
        return !findZonesContainingPoint(companyId, latitude, longitude).isEmpty();
    }

    @Override
    public GeofenceEvent trackEvent(GeofenceEvent event) {
        event.setTimestamp(LocalDateTime.now());
        event.setCreatedAt(LocalDateTime.now());

        log.info("Tracking geofence event: {} for device {} in zone {}",
                event.getEventType(),
                event.getDevice().getId(),
                event.getZone().getId());

        return eventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceEvent> getDeviceEvents(Long deviceId, int page, int size) {
        Page<GeofenceEvent> events = eventRepository.findByDevice_Id(
                deviceId,
                PageRequest.of(page, size));
        return events.getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceEvent> getZoneEvents(Long zoneId, int page, int size) {
        Page<GeofenceEvent> events = eventRepository.findByZone_Id(
                zoneId,
                PageRequest.of(page, size));
        return events.getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getZoneStatistics(Long zoneId) {
        GeofenceZone zone = getZone(zoneId);
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);

        long enterEvents = eventRepository.countByZone_IdAndEventTypeAndTimestampAfter(
                zoneId, GeofenceEventType.ENTER, last7Days);

        long exitEvents = eventRepository.countByZone_IdAndEventTypeAndTimestampAfter(
                zoneId, GeofenceEventType.EXIT, last7Days);

        long dwellEvents = eventRepository.countByZone_IdAndEventTypeAndTimestampAfter(
                zoneId, GeofenceEventType.DWELL, last7Days);

        Map<String, Object> stats = new HashMap<>();
        stats.put("zoneId", zoneId);
        stats.put("zoneName", zone.getName());
        stats.put("enterEvents", enterEvents);
        stats.put("exitEvents", exitEvents);
        stats.put("dwellEvents", dwellEvents);
        stats.put("totalEvents", enterEvents + exitEvents + dwellEvents);
        stats.put("period", "Last 7 days");

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCompanyGeofenceStats(Long companyId) {
        long totalZones = zoneRepository.countByCompany_Id(companyId);
        long activeZones = zoneRepository.countByCompany_IdAndIsActiveTrue(companyId);

        List<GeofenceZone> zones = getActiveZonesByCompany(companyId);

        Map<String, Long> zonesByType = zones.stream()
                .collect(Collectors.groupingBy(
                        zone -> zone.getType().name(),
                        Collectors.counting()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("companyId", companyId);
        stats.put("totalZones", totalZones);
        stats.put("activeZones", activeZones);
        stats.put("zonesByType", zonesByType);

        return stats;
    }
}
