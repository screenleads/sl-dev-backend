package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.GeofenceRule;
import com.screenleads.backend.app.domain.repository.GeofenceRuleRepository;
import com.screenleads.backend.app.service.GeofenceRuleService;
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
public class GeofenceRuleServiceImpl implements GeofenceRuleService {

    private final GeofenceRuleRepository geofenceRuleRepository;

    @Override
    @Transactional
    public GeofenceRule createRule(GeofenceRule rule) {
        log.info("Creating new geofence rule for promotion ID: {} and zone ID: {}", 
            rule.getPromotion().getId(), rule.getZone().getId());
        return geofenceRuleRepository.save(rule);
    }

    @Override
    @Transactional
    public GeofenceRule updateRule(Long id, GeofenceRule rule) {
        GeofenceRule existing = geofenceRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Geofence rule not found: " + id));

        log.info("Updating geofence rule ID: {}", id);

        existing.setPromotion(rule.getPromotion());
        existing.setZone(rule.getZone());
        existing.setRuleType(rule.getRuleType());
        existing.setPriority(rule.getPriority());
        existing.setIsActive(rule.getIsActive());

        return geofenceRuleRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        log.info("Deleting geofence rule ID: {}", id);
        geofenceRuleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GeofenceRule> getRuleById(Long id) {
        return geofenceRuleRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceRule> getRulesByCompany(Long companyId) {
        return geofenceRuleRepository.findByCompany_IdOrderByPriorityDescCreatedAtDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceRule> getActiveRulesByCompany(Long companyId) {
        return geofenceRuleRepository.findByCompany_IdAndIsActiveTrueOrderByPriorityDescCreatedAtDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceRule> getRulesByZone(Long zoneId) {
        return geofenceRuleRepository.findByZone_IdOrderByPriorityDesc(zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceRule> getActiveRulesByZone(Long zoneId) {
        return geofenceRuleRepository.findByZone_IdAndIsActiveTrueOrderByPriorityDesc(zoneId);
    }

    @Override
    @Transactional
    public GeofenceRule toggleRuleActive(Long id, boolean active) {
        GeofenceRule rule = geofenceRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Geofence rule not found: " + id));

        log.info("Toggling geofence rule {} active status to: {}", id, active);
        rule.setIsActive(active);
        return geofenceRuleRepository.save(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceRule> findApplicableRules(Long zoneId) {
        log.debug("Finding applicable rules for zone {}", zoneId);
        
        // Get active rules for the zone
        List<GeofenceRule> rules = getActiveRulesByZone(zoneId);
        
        // Return sorted by priority
        return rules.stream()
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long countActiveRules(Long companyId) {
        return geofenceRuleRepository.countByCompany_IdAndIsActiveTrue(companyId);
    }
}
