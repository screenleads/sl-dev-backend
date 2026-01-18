package com.screenleads.backend.app.controller;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.AdviceImpression;
import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;
import com.screenleads.backend.app.domain.repositories.DeviceRepository;
import com.screenleads.backend.app.dto.request.TrackImpressionRequest;
import com.screenleads.backend.app.service.AdviceImpressionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for tracking and retrieving Advice impressions
 */
@RestController
@RequestMapping("/api/analytics/impressions")
@RequiredArgsConstructor
@Slf4j
public class AdviceImpressionController {

    private final AdviceImpressionService adviceImpressionService;
    private final AdviceRepository adviceRepository;
    private final DeviceRepository deviceRepository;
    private final CustomerRepository customerRepository;

    /**
     * Track a new impression (when an Advice is displayed on a Device)
     */
    @PostMapping
    public ResponseEntity<?> trackImpression(@Valid @RequestBody TrackImpressionRequest request) {
        try {
            // Validate Advice exists
            Advice advice = adviceRepository.findById(request.getAdviceId())
                .orElseThrow(() -> new RuntimeException("Advice not found: " + request.getAdviceId()));

            // Validate Device exists
            Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found: " + request.getDeviceId()));

            // Optional: Validate Customer if provided
            Customer customer = null;
            if (request.getCustomerId() != null) {
                customer = customerRepository.findById(request.getCustomerId())
                    .orElse(null);
            }

            // Create impression
            AdviceImpression impression = AdviceImpression.builder()
                .advice(advice)
                .device(device)
                .customer(customer)
                .durationSeconds(request.getDurationSeconds())
                .sessionId(request.getSessionId())
                .ipAddress(request.getIpAddress())
                .wasInteractive(request.getWasInteractive())
                .userAgent(request.getUserAgent())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

            AdviceImpression savedImpression = adviceImpressionService.createImpression(impression);

            log.info("Tracked impression ID {} for advice ID {} on device ID {}",
                savedImpression.getId(), request.getAdviceId(), request.getDeviceId());

            Map<String, Object> response = new HashMap<>();
            response.put("impressionId", savedImpression.getId());
            response.put("timestamp", savedImpression.getTimestamp());
            response.put("message", "Impression tracked successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.error("Error tracking impression: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all impressions for a specific Advice
     */
    @GetMapping("/advice/{adviceId}")
    public ResponseEntity<List<AdviceImpression>> getImpressionsByAdvice(@PathVariable Long adviceId) {
        List<AdviceImpression> impressions = adviceImpressionService.getImpressionsByAdviceId(adviceId);
        return ResponseEntity.ok(impressions);
    }

    /**
     * Get impression statistics for an Advice
     */
    @GetMapping("/advice/{adviceId}/stats")
    public ResponseEntity<Map<String, Object>> getImpressionStats(@PathVariable Long adviceId) {
        Long totalImpressions = adviceImpressionService.countImpressionsByAdviceId(adviceId);
        Long interactiveImpressions = adviceImpressionService.countInteractiveImpressionsByAdviceId(adviceId);
        Long uniqueCustomers = adviceImpressionService.getUniqueCustomerCountByAdviceId(adviceId);
        Double avgDuration = adviceImpressionService.calculateAverageDurationByAdviceId(adviceId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("adviceId", adviceId);
        stats.put("totalImpressions", totalImpressions);
        stats.put("interactiveImpressions", interactiveImpressions);
        stats.put("uniqueCustomers", uniqueCustomers);
        stats.put("averageDurationSeconds", avgDuration);
        
        if (totalImpressions > 0) {
            double interactionRate = (interactiveImpressions.doubleValue() / totalImpressions.doubleValue()) * 100;
            stats.put("interactionRatePercent", Math.round(interactionRate * 100.0) / 100.0);
        } else {
            stats.put("interactionRatePercent", 0.0);
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * Get impressions for a specific Device
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<AdviceImpression>> getImpressionsByDevice(@PathVariable Long deviceId) {
        List<AdviceImpression> impressions = adviceImpressionService.getImpressionsByDeviceId(deviceId);
        return ResponseEntity.ok(impressions);
    }

    /**
     * Get impressions for a specific Customer
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AdviceImpression>> getImpressionsByCustomer(@PathVariable Long customerId) {
        List<AdviceImpression> impressions = adviceImpressionService.getImpressionsByCustomerId(customerId);
        return ResponseEntity.ok(impressions);
    }

    /**
     * Get impressions by session ID
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<AdviceImpression>> getImpressionsBySession(@PathVariable String sessionId) {
        List<AdviceImpression> impressions = adviceImpressionService.getImpressionsBySessionId(sessionId);
        return ResponseEntity.ok(impressions);
    }
}
