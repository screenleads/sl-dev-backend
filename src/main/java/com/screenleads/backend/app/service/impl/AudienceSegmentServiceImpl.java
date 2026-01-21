package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.AudienceSegment;
import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.repository.AudienceSegmentRepository;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;
import com.screenleads.backend.app.service.AudienceSegmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudienceSegmentServiceImpl implements AudienceSegmentService {

    private final AudienceSegmentRepository audienceSegmentRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public AudienceSegment createSegment(AudienceSegment segment) {
        log.info("Creating new audience segment: {} for company ID: {}",
                segment.getName(), segment.getCompany().getId());

        AudienceSegment saved = audienceSegmentRepository.save(segment);

        // Calculate initial customer count
        updateSegmentCustomerCount(saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public AudienceSegment updateSegment(Long id, AudienceSegment segment) {
        AudienceSegment existing = audienceSegmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audience segment not found: " + id));

        log.info("Updating audience segment ID: {}", id);

        existing.setName(segment.getName());
        existing.setDescription(segment.getDescription());
        existing.setFilterRules(segment.getFilterRules());
        existing.setIsActive(segment.getIsActive());
        existing.setMetadata(segment.getMetadata());

        AudienceSegment updated = audienceSegmentRepository.save(existing);

        // Recalculate customer count if filter rules changed
        updateSegmentCustomerCount(updated.getId());

        return updated;
    }

    @Override
    @Transactional
    public void deleteSegment(Long id) {
        log.info("Deleting audience segment ID: {}", id);
        audienceSegmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AudienceSegment> getSegmentById(Long id) {
        return audienceSegmentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AudienceSegment> getSegmentsByCompany(Long companyId) {
        return audienceSegmentRepository.findByCompany_IdOrderByCreatedAtDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AudienceSegment> getActiveSegmentsByCompany(Long companyId) {
        return audienceSegmentRepository.findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean customerMatchesSegment(Customer customer, AudienceSegment segment) {
        if (segment.getFilterRules() == null || segment.getFilterRules().isEmpty()) {
            return true; // No filters means all customers match
        }

        Map<String, Object> rules = segment.getFilterRules();

        // Check minimum redemptions (totalRedemptions)
        if (rules.containsKey("minRedemptions")) {
            Integer minRedemptions = getIntValue(rules.get("minRedemptions"));
            if (minRedemptions != null
                    && (customer.getTotalRedemptions() == null || customer.getTotalRedemptions() < minRedemptions)) {
                return false;
            }
        }

        // Check minimum lifetime value (lifetimeValue)
        if (rules.containsKey("minLifetimeValue")) {
            Double minValue = getDoubleValue(rules.get("minLifetimeValue"));
            if (minValue != null
                    && (customer.getLifetimeValue() == null || customer.getLifetimeValue().doubleValue() < minValue)) {
                return false;
            }
        }

        // Check last interaction days (lastInteractionAt)
        if (rules.containsKey("lastInteractionDays")) {
            Integer lastInteractionDays = getIntValue(rules.get("lastInteractionDays"));
            if (lastInteractionDays != null && customer.getLastInteractionAt() != null) {
                Instant cutoffDate = Instant.now().minus(lastInteractionDays, java.time.temporal.ChronoUnit.DAYS);
                if (customer.getLastInteractionAt().isBefore(cutoffDate)) {
                    return false;
                }
            }
        }

        // Check email verified status
        if (rules.containsKey("emailVerified")) {
            Boolean emailVerified = (Boolean) rules.get("emailVerified");
            if (emailVerified != null && !emailVerified.equals(customer.getEmailVerified())) {
                return false;
            }
        }

        // Check marketing opt-in status
        if (rules.containsKey("marketingOptIn")) {
            Boolean marketingOptIn = (Boolean) rules.get("marketingOptIn");
            if (marketingOptIn != null && !marketingOptIn.equals(customer.getMarketingOptIn())) {
                return false;
            }
        }

        // Check segment (COLD, WARM, HOT, VIP)
        if (rules.containsKey("segment")) {
            String segmentStr = (String) rules.get("segment");
            if (segmentStr != null && customer.getSegment() != null) {
                if (!customer.getSegment().name().equals(segmentStr)) {
                    return false;
                }
            }
        }

        // Check engagement score range
        if (rules.containsKey("minEngagementScore")) {
            Integer minScore = getIntValue(rules.get("minEngagementScore"));
            if (minScore != null
                    && (customer.getEngagementScore() == null || customer.getEngagementScore() < minScore)) {
                return false;
            }
        }
        if (rules.containsKey("maxEngagementScore")) {
            Integer maxScore = getIntValue(rules.get("maxEngagementScore"));
            if (maxScore != null
                    && (customer.getEngagementScore() == null || customer.getEngagementScore() > maxScore)) {
                return false;
            }
        }

        // Check age range (calculated from birthDate)
        if (customer.getBirthDate() != null) {
            int age = java.time.Period.between(customer.getBirthDate(), java.time.LocalDate.now()).getYears();

            if (rules.containsKey("minAge")) {
                Integer minAge = getIntValue(rules.get("minAge"));
                if (minAge != null && age < minAge) {
                    return false;
                }
            }
            if (rules.containsKey("maxAge")) {
                Integer maxAge = getIntValue(rules.get("maxAge"));
                if (maxAge != null && age > maxAge) {
                    return false;
                }
            }
        }

        // Check city filter
        if (rules.containsKey("city")) {
            String cityFilter = (String) rules.get("city");
            if (cityFilter != null && customer.getCity() != null) {
                if (!customer.getCity().equalsIgnoreCase(cityFilter)) {
                    return false;
                }
            }
        }

        // Check country filter
        if (rules.containsKey("country")) {
            String countryFilter = (String) rules.get("country");
            if (countryFilter != null && customer.getCountry() != null) {
                if (!customer.getCountry().equalsIgnoreCase(countryFilter)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getCustomersInSegment(Long segmentId) {
        AudienceSegment segment = audienceSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Audience segment not found: " + segmentId));

        List<Customer> allCustomers = customerRepository.findByCompanyId(segment.getCompany().getId());

        return allCustomers.stream()
                .filter(customer -> customerMatchesSegment(customer, segment))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateSegmentCustomerCount(Long segmentId) {
        AudienceSegment segment = audienceSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Audience segment not found: " + segmentId));

        List<Customer> matchingCustomers = getCustomersInSegment(segmentId);

        segment.setCustomerCount((long) matchingCustomers.size());
        segment.setLastCalculatedAt(LocalDateTime.now());

        audienceSegmentRepository.save(segment);

        log.debug("Updated customer count for segment ID {}: {} customers", segmentId, matchingCustomers.size());
    }

    @Override
    @Transactional
    public void recalculateAllSegmentCounts() {
        log.info("Starting recalculation of segment customer counts");

        List<AudienceSegment> segments = audienceSegmentRepository.findSegmentsNeedingRecalculation();

        log.info("Found {} segments needing recalculation", segments.size());

        for (AudienceSegment segment : segments) {
            try {
                updateSegmentCustomerCount(segment.getId());
            } catch (Exception e) {
                log.error("Error recalculating count for segment ID {}: {}", segment.getId(), e.getMessage());
            }
        }

        log.info("Completed segment count recalculation");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> previewSegmentMatch(Long companyId, AudienceSegment segment) {
        List<Customer> allCustomers = customerRepository.findByCompanyId(companyId);

        return allCustomers.stream()
                .filter(customer -> customerMatchesSegment(customer, segment))
                .limit(100) // Limit preview to 100 customers
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AudienceSegment toggleSegmentActive(Long id, boolean active) {
        AudienceSegment segment = audienceSegmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audience segment not found: " + id));

        segment.setIsActive(active);
        return audienceSegmentRepository.save(segment);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSegmentNameUnique(String name, Long companyId, Long excludeId) {
        Optional<AudienceSegment> existing = audienceSegmentRepository.findByNameAndCompany_Id(name, companyId);

        if (existing.isEmpty()) {
            return true;
        }

        // If updating, check if it's the same segment
        return excludeId != null && existing.get().getId().equals(excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countCustomersInSegment(Long segmentId) {
        List<Customer> customers = getCustomersInSegment(segmentId);
        return (long) customers.size();
    }

    @Override
    @Transactional
    public void rebuildSegment(Long segmentId) {
        AudienceSegment segment = audienceSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Audience segment not found: " + segmentId));

        log.info("Rebuilding audience segment: {} (ID: {})", segment.getName(), segmentId);

        // Recalculate customer count
        updateSegmentCustomerCount(segmentId);

        log.info("Segment {} rebuilt successfully with {} members", 
            segment.getName(), segment.getCustomerCount());
    }

    // Helper methods
    private Integer getIntValue(Object value) {
        if (value == null)
            return null;
        if (value instanceof Integer)
            return (Integer) value;
        if (value instanceof Number)
            return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double getDoubleValue(Object value) {
        if (value == null)
            return null;
        if (value instanceof Double)
            return (Double) value;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
