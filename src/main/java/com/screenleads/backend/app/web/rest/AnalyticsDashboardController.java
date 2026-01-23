package com.screenleads.backend.app.web.rest;

import com.screenleads.backend.app.service.AnalyticsDashboardService;
import com.screenleads.backend.app.service.AnalyticsDashboardService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Advanced Analytics Dashboard KPIs
 */
@RestController
@RequestMapping("/api/analytics/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsDashboardController {

    private final AnalyticsDashboardService analyticsDashboardService;

    /**
     * GET /api/analytics/dashboard/summary
     * Get overall dashboard summary with key metrics
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("REST request to get dashboard summary for company: {}, period: {} to {}",
                companyId, startDate, endDate);

        DashboardSummary summary = analyticsDashboardService.getDashboardSummary(companyId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/analytics/dashboard/top-promotions/conversion
     * Get top promotions ranked by conversion rate
     */
    @GetMapping("/top-promotions/conversion")
    public ResponseEntity<List<TopPromotionDTO>> getTopPromotionsByConversion(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {

        log.debug("REST request to get top {} promotions by conversion for company: {}", limit, companyId);

        List<TopPromotionDTO> topPromotions = analyticsDashboardService
                .getTopPromotionsByConversion(companyId, startDate, endDate, limit);
        return ResponseEntity.ok(topPromotions);
    }

    /**
     * GET /api/analytics/dashboard/top-promotions/revenue
     * Get top promotions ranked by total revenue
     */
    @GetMapping("/top-promotions/revenue")
    public ResponseEntity<List<TopPromotionDTO>> getTopPromotionsByRevenue(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {

        log.debug("REST request to get top {} promotions by revenue for company: {}", limit, companyId);

        List<TopPromotionDTO> topPromotions = analyticsDashboardService
                .getTopPromotionsByRevenue(companyId, startDate, endDate, limit);
        return ResponseEntity.ok(topPromotions);
    }

    /**
     * GET /api/analytics/dashboard/roi
     * Get ROI calculation for all promotions
     */
    @GetMapping("/roi")
    public ResponseEntity<List<PromotionROI>> getPromotionROI(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("REST request to get promotion ROI for company: {}", companyId);

        List<PromotionROI> roi = analyticsDashboardService.getPromotionROI(companyId, startDate, endDate);
        return ResponseEntity.ok(roi);
    }

    /**
     * GET /api/analytics/dashboard/device-performance
     * Get performance metrics by device
     */
    @GetMapping("/device-performance")
    public ResponseEntity<List<DevicePerformance>> getDevicePerformance(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("REST request to get device performance for company: {}", companyId);

        List<DevicePerformance> performance = analyticsDashboardService
                .getDevicePerformance(companyId, startDate, endDate);
        return ResponseEntity.ok(performance);
    }

    /**
     * GET /api/analytics/dashboard/hourly-distribution
     * Get hourly distribution of impressions and conversions
     */
    @GetMapping("/hourly-distribution")
    public ResponseEntity<Map<Integer, HourlyMetrics>> getHourlyDistribution(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("REST request to get hourly distribution for company: {}", companyId);

        Map<Integer, HourlyMetrics> distribution = analyticsDashboardService
                .getHourlyDistribution(companyId, startDate, endDate);
        return ResponseEntity.ok(distribution);
    }

    /**
     * GET /api/analytics/dashboard/geographic-metrics
     * Get metrics by geofence zone
     */
    @GetMapping("/geographic-metrics")
    public ResponseEntity<List<GeographicMetrics>> getGeographicMetrics(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("REST request to get geographic metrics for company: {}", companyId);

        List<GeographicMetrics> metrics = analyticsDashboardService
                .getMetricsByGeofenceZone(companyId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/analytics/dashboard/daily-trends
     * Get daily trends for key metrics
     */
    @GetMapping("/daily-trends")
    public ResponseEntity<List<DailyTrendDTO>> getDailyTrends(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("REST request to get daily trends for company: {}", companyId);

        List<DailyTrendDTO> trends = analyticsDashboardService
                .getDailyTrends(companyId, startDate, endDate);
        return ResponseEntity.ok(trends);
    }
}
