package com.screenleads.backend.app.controller;

import com.screenleads.backend.app.domain.model.AdviceImpression;
import com.screenleads.backend.app.domain.model.AdviceInteraction;
import com.screenleads.backend.app.domain.model.Customer;
import com.screenleads.backend.app.domain.model.InteractionType;
import com.screenleads.backend.app.domain.repository.AdviceImpressionRepository;
import com.screenleads.backend.app.domain.repositories.CustomerRepository;
import com.screenleads.backend.app.dto.request.TrackInteractionRequest;
import com.screenleads.backend.app.service.AdviceInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for tracking and retrieving Advice interactions
 */
@RestController
@RequestMapping("/api/analytics/interactions")
@RequiredArgsConstructor
@Slf4j
public class AdviceInteractionController {

    private final AdviceInteractionService adviceInteractionService;
    private final AdviceImpressionRepository adviceImpressionRepository;
    private final CustomerRepository customerRepository;

    /**
     * Track a new interaction (when a user interacts with an Advice after viewing it)
     */
    @PostMapping
    public ResponseEntity<?> trackInteraction(@Valid @RequestBody TrackInteractionRequest request) {
        try {
            // Validate Impression exists
            AdviceImpression impression = adviceImpressionRepository.findById(request.getImpressionId())
                .orElseThrow(() -> new RuntimeException("Impression not found: " + request.getImpressionId()));

            // Optional: Validate Customer if provided
            Customer customer = null;
            if (request.getCustomerId() != null) {
                customer = customerRepository.findById(request.getCustomerId())
                    .orElse(null);
            }

            // Create interaction
            AdviceInteraction interaction = AdviceInteraction.builder()
                .impression(impression)
                .customer(customer)
                .type(request.getType())
                .details(request.getDetails())
                .durationSeconds(request.getDurationSeconds())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .isConversion(request.getIsConversion())
                .build();

            AdviceInteraction savedInteraction = adviceInteractionService.createInteraction(interaction);

            log.info("Tracked interaction ID {} type {} for impression ID {}",
                savedInteraction.getId(), request.getType(), request.getImpressionId());

            Map<String, Object> response = new HashMap<>();
            response.put("interactionId", savedInteraction.getId());
            response.put("timestamp", savedInteraction.getTimestamp());
            response.put("type", savedInteraction.getType());
            response.put("message", "Interaction tracked successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.error("Error tracking interaction: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all interactions for a specific impression
     */
    @GetMapping("/impression/{impressionId}")
    public ResponseEntity<List<AdviceInteraction>> getInteractionsByImpression(@PathVariable Long impressionId) {
        List<AdviceInteraction> interactions = adviceInteractionService.getInteractionsByImpressionId(impressionId);
        return ResponseEntity.ok(interactions);
    }

    /**
     * Get all interactions for a specific Advice
     */
    @GetMapping("/advice/{adviceId}")
    public ResponseEntity<List<AdviceInteraction>> getInteractionsByAdvice(@PathVariable Long adviceId) {
        List<AdviceInteraction> interactions = adviceInteractionService.getInteractionsByAdviceId(adviceId);
        return ResponseEntity.ok(interactions);
    }

    /**
     * Get interaction statistics for an Advice
     */
    @GetMapping("/advice/{adviceId}/stats")
    public ResponseEntity<Map<String, Object>> getInteractionStats(@PathVariable Long adviceId) {
        Long totalInteractions = adviceInteractionService.countInteractionsByAdviceId(adviceId);
        Long totalConversions = adviceInteractionService.countConversionsByAdviceId(adviceId);
        Long uniqueCustomers = adviceInteractionService.getUniqueCustomerCountByAdviceId(adviceId);
        Double avgDuration = adviceInteractionService.calculateAverageDurationByAdviceId(adviceId);

        // Count by interaction type
        Map<String, Long> byType = new HashMap<>();
        for (InteractionType type : InteractionType.values()) {
            Long count = adviceInteractionService.countInteractionsByAdviceIdAndType(adviceId, type);
            if (count > 0) {
                byType.put(type.name(), count);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("adviceId", adviceId);
        stats.put("totalInteractions", totalInteractions);
        stats.put("totalConversions", totalConversions);
        stats.put("uniqueCustomers", uniqueCustomers);
        stats.put("averageDurationSeconds", avgDuration);
        stats.put("byType", byType);
        
        if (totalInteractions > 0) {
            double conversionRate = (totalConversions.doubleValue() / totalInteractions.doubleValue()) * 100;
            stats.put("conversionRatePercent", Math.round(conversionRate * 100.0) / 100.0);
        } else {
            stats.put("conversionRatePercent", 0.0);
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * Get interactions for a specific Customer
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AdviceInteraction>> getInteractionsByCustomer(@PathVariable Long customerId) {
        List<AdviceInteraction> interactions = adviceInteractionService.getInteractionsByCustomerId(customerId);
        return ResponseEntity.ok(interactions);
    }

    /**
     * Get interactions by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<AdviceInteraction>> getInteractionsByType(@PathVariable InteractionType type) {
        List<AdviceInteraction> interactions = adviceInteractionService.getInteractionsByType(type);
        return ResponseEntity.ok(interactions);
    }
}
