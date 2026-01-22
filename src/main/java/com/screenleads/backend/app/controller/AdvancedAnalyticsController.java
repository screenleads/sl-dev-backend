package com.screenleads.backend.app.controller;

import com.screenleads.backend.app.service.AdvancedAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Advanced Analytics
 */
@RestController
@RequestMapping("/api/analytics/advanced")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins:*}")
public class AdvancedAnalyticsController {

    private final AdvancedAnalyticsService advancedAnalyticsService;

    /**
     * Get cohort analysis
     */
    @GetMapping("/company/{companyId}/cohort")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<Map<String, Object>> getCohortAnalysis(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "7") int periodDays) {

        log.info("GET /api/analytics/advanced/company/{}/cohort", companyId);
        Map<String, Object> cohortData = advancedAnalyticsService.getCohortAnalysis(
                companyId, startDate, endDate, periodDays);
        return ResponseEntity.ok(cohortData);
    }

    /**
     * Get funnel analysis
     */
    @GetMapping("/company/{companyId}/funnel")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<Map<String, Object>> getFunnelAnalysis(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /api/analytics/advanced/company/{}/funnel", companyId);
        Map<String, Object> funnelData = advancedAnalyticsService.getFunnelAnalysis(
                companyId, startDate, endDate);
        return ResponseEntity.ok(funnelData);
    }

    /**
     * Get attribution analysis
     */
    @GetMapping("/company/{companyId}/attribution")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<Map<String, Object>> getAttributionAnalysis(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "LAST_TOUCH") String model) {

        log.info("GET /api/analytics/advanced/company/{}/attribution?model={}", companyId, model);
        Map<String, Object> attributionData = advancedAnalyticsService.getAttributionAnalysis(
                companyId, startDate, endDate, model);
        return ResponseEntity.ok(attributionData);
    }

    /**
     * Get time series data
     */
    @GetMapping("/company/{companyId}/time-series")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<List<Map<String, Object>>> getTimeSeriesData(
            @PathVariable Long companyId,
            @RequestParam String metric,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "DAILY") String granularity) {

        log.info("GET /api/analytics/advanced/company/{}/time-series?metric={}&granularity={}",
                companyId, metric, granularity);
        List<Map<String, Object>> timeSeriesData = advancedAnalyticsService.getTimeSeriesData(
                companyId, metric, startDate, endDate, granularity);
        return ResponseEntity.ok(timeSeriesData);
    }

    /**
     * Get customer lifetime value
     */
    @GetMapping("/company/{companyId}/clv")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<Map<String, Object>> getCustomerLifetimeValue(
            @PathVariable Long companyId,
            @RequestParam(required = false) Long customerId) {

        log.info("GET /api/analytics/advanced/company/{}/clv?customerId={}", companyId, customerId);
        Map<String, Object> clvData = advancedAnalyticsService.getCustomerLifetimeValue(
                companyId, customerId);
        return ResponseEntity.ok(clvData);
    }

    /**
     * Get conversion paths
     */
    @GetMapping("/company/{companyId}/conversion-paths")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<List<Map<String, Object>>> getConversionPaths(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "5") int maxPathLength) {

        log.info("GET /api/analytics/advanced/company/{}/conversion-paths", companyId);
        List<Map<String, Object>> paths = advancedAnalyticsService.getConversionPaths(
                companyId, startDate, endDate, maxPathLength);
        return ResponseEntity.ok(paths);
    }

    /**
     * Get device performance comparison
     */
    @GetMapping("/company/{companyId}/device-performance")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<Map<String, Object>> getDevicePerformance(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /api/analytics/advanced/company/{}/device-performance", companyId);
        Map<String, Object> deviceData = advancedAnalyticsService.getDevicePerformance(
                companyId, startDate, endDate);
        return ResponseEntity.ok(deviceData);
    }

    /**
     * Get promotion comparison
     */
    @GetMapping("/company/{companyId}/promotion-comparison")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<List<Map<String, Object>>> getPromotionComparison(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /api/analytics/advanced/company/{}/promotion-comparison", companyId);
        List<Map<String, Object>> comparisonData = advancedAnalyticsService.getPromotionComparison(
                companyId, startDate, endDate);
        return ResponseEntity.ok(comparisonData);
    }

    /**
     * Get real-time dashboard metrics
     */
    @GetMapping("/company/{companyId}/realtime")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<Map<String, Object>> getRealTimeDashboard(@PathVariable Long companyId) {
        log.info("GET /api/analytics/advanced/company/{}/realtime", companyId);
        Map<String, Object> realtimeData = advancedAnalyticsService.getRealTimeDashboard(companyId);
        return ResponseEntity.ok(realtimeData);
    }

    /**
     * Export analytics data to CSV
     */
    @GetMapping("/company/{companyId}/export")
    @PreAuthorize("hasAuthority('analytics:read')")
    public ResponseEntity<byte[]> exportAnalytics(
            @PathVariable Long companyId,
            @RequestParam String reportType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /api/analytics/advanced/company/{}/export?reportType={}", companyId, reportType);

        byte[] csvData = advancedAnalyticsService.exportAnalyticsToCsv(
                companyId, reportType, startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment",
                String.format("analytics_%s_%s.csv", reportType, LocalDate.now()));

        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }
}
