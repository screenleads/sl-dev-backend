package com.screenleads.backend.app.controller;

import com.screenleads.backend.app.domain.model.PromotionMetrics;
import com.screenleads.backend.app.scheduler.MetricsCalculationScheduler;
import com.screenleads.backend.app.service.PromotionMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Analytics Dashboard - provides aggregated metrics and insights
 */
@RestController
@RequestMapping("/api/analytics/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsDashboardController {

    private final PromotionMetricsService promotionMetricsService;
    private final MetricsCalculationScheduler metricsCalculationScheduler;

    /**
     * Get dashboard overview with key metrics
     */
    @GetMapping("/overview")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('analytics','read')")
    public ResponseEntity<Map<String, Object>> getDashboardOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // Default to last 30 days if not specified
        if (endDate == null) {
            endDate = LocalDate.now().minusDays(1); // Yesterday
        }
        if (startDate == null) {
            startDate = endDate.minusDays(29); // 30 days including end date
        }

        log.info("Getting dashboard overview for period: {} to {}", startDate, endDate);

        Map<String, Object> overview = new HashMap<>();
        overview.put("startDate", startDate);
        overview.put("endDate", endDate);
        overview.put("topConversions", promotionMetricsService.getTopPerformingByConversions(startDate, endDate, 10));
        overview.put("topConversionRate", promotionMetricsService.getTopPerformingByConversionRate(startDate, endDate, 10));
        overview.put("latestMetricDate", promotionMetricsService.getLatestMetricDate().orElse(null));

        return ResponseEntity.ok(overview);
    }

    /**
     * Get metrics for a specific promotion/advice
     */
    @GetMapping("/promotion/{adviceId}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('analytics','read')")
    public ResponseEntity<Map<String, Object>> getPromotionMetrics(
            @PathVariable Long adviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<PromotionMetrics> metrics;
        
        if (startDate != null && endDate != null) {
            metrics = promotionMetricsService.getMetricsByAdviceAndDateRange(adviceId, startDate, endDate);
        } else {
            metrics = promotionMetricsService.getMetricsByAdvice(adviceId);
        }

        // Calculate totals
        Long totalImpressions = metrics.stream()
            .mapToLong(PromotionMetrics::getTotalImpressions)
            .sum();
        
        Long totalInteractions = metrics.stream()
            .mapToLong(PromotionMetrics::getTotalInteractions)
            .sum();
        
        Long totalConversions = metrics.stream()
            .mapToLong(PromotionMetrics::getTotalConversions)
            .sum();

        Map<String, Object> response = new HashMap<>();
        response.put("adviceId", adviceId);
        response.put("metrics", metrics);
        response.put("totals", Map.of(
            "impressions", totalImpressions,
            "interactions", totalInteractions,
            "conversions", totalConversions,
            "overallConversionRate", totalImpressions > 0 
                ? Math.round((totalConversions.doubleValue() / totalImpressions.doubleValue()) * 10000.0) / 100.0
                : 0.0
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Get metrics for a specific date
     */
    @GetMapping("/date/{date}")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('analytics','read')")
    public ResponseEntity<List<PromotionMetrics>> getMetricsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<PromotionMetrics> metrics = promotionMetricsService.getMetricsByDate(date);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get top performing promotions by conversion rate
     */
    @GetMapping("/top/conversion-rate")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('analytics','read')")
    public ResponseEntity<List<PromotionMetrics>> getTopByConversionRate(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (endDate == null) {
            endDate = LocalDate.now().minusDays(1);
        }
        if (startDate == null) {
            startDate = endDate.minusDays(29);
        }

        List<PromotionMetrics> topPerformers = 
            promotionMetricsService.getTopPerformingByConversionRate(startDate, endDate, limit);
        
        return ResponseEntity.ok(topPerformers);
    }

    /**
     * Get top performing promotions by total conversions
     */
    @GetMapping("/top/conversions")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('analytics','read')")
    public ResponseEntity<List<PromotionMetrics>> getTopByConversions(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (endDate == null) {
            endDate = LocalDate.now().minusDays(1);
        }
        if (startDate == null) {
            startDate = endDate.minusDays(29);
        }

        List<PromotionMetrics> topPerformers = 
            promotionMetricsService.getTopPerformingByConversions(startDate, endDate, limit);
        
        return ResponseEntity.ok(topPerformers);
    }

    /**
     * Manual trigger to calculate metrics for a specific date (admin endpoint)
     */
    @PostMapping("/calculate")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> calculateMetricsForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Manual trigger to calculate metrics for date: {}", date);
        
        try {
            metricsCalculationScheduler.calculateMetricsForDate(date);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Metrics calculation completed for date: " + date);
            response.put("date", date);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating metrics for date {}: {}", date, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error calculating metrics: " + e.getMessage());
            response.put("date", date);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get latest metric calculation date
     */
    @GetMapping("/latest-date")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.can('analytics','read')")
    public ResponseEntity<Map<String, Object>> getLatestMetricDate() {
        Optional<LocalDate> latestDate = promotionMetricsService.getLatestMetricDate();
        
        Map<String, Object> response = new HashMap<>();
        response.put("latestMetricDate", latestDate.orElse(null));
        response.put("hasMetrics", latestDate.isPresent());
        
        return ResponseEntity.ok(response);
    }
}
