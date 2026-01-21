package com.screenleads.backend.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Advanced Analytics features
 * Provides cohort analysis, funnel metrics, attribution, and time-series aggregations
 */
public interface AdvancedAnalyticsService {

    /**
     * Cohort Analysis: Group customers by acquisition date and track their behavior over time
     * 
     * @param companyId Company ID
     * @param startDate Start date for cohort definition
     * @param endDate End date for cohort definition
     * @param periodDays Period length in days (e.g., 7 for weekly, 30 for monthly)
     * @return Map of cohort metrics (retention rates, conversion rates, etc.)
     */
    Map<String, Object> getCohortAnalysis(Long companyId, LocalDate startDate, LocalDate endDate, int periodDays);

    /**
     * Funnel Analysis: Track conversion rates through multiple stages
     * 
     * @param companyId Company ID
     * @param startDate Start date
     * @param endDate End date
     * @return Funnel metrics with drop-off rates at each stage
     */
    Map<String, Object> getFunnelAnalysis(Long companyId, LocalDate startDate, LocalDate endDate);

    /**
     * Attribution Analysis: Determine which touchpoints led to conversions
     * 
     * @param companyId Company ID
     * @param startDate Start date
     * @param endDate End date
     * @param attributionModel Attribution model (FIRST_TOUCH, LAST_TOUCH, LINEAR)
     * @return Attribution metrics by channel/promotion
     */
    Map<String, Object> getAttributionAnalysis(
        Long companyId, 
        LocalDate startDate, 
        LocalDate endDate, 
        String attributionModel
    );

    /**
     * Time Series Aggregation: Get metrics aggregated by time period
     * 
     * @param companyId Company ID
     * @param metric Metric to aggregate (impressions, conversions, revenue, etc.)
     * @param startDate Start date
     * @param endDate End date
     * @param granularity Time granularity (HOURLY, DAILY, WEEKLY, MONTHLY)
     * @return Time series data points
     */
    List<Map<String, Object>> getTimeSeriesData(
        Long companyId,
        String metric,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String granularity
    );

    /**
     * Customer Lifetime Value (CLV) calculation
     * 
     * @param companyId Company ID
     * @param customerId Customer ID (optional, if null calculates average CLV)
     * @return CLV metrics
     */
    Map<String, Object> getCustomerLifetimeValue(Long companyId, Long customerId);

    /**
     * Conversion Path Analysis: Identify common paths to conversion
     * 
     * @param companyId Company ID
     * @param startDate Start date
     * @param endDate End date
     * @param maxPathLength Maximum path length to analyze
     * @return Top conversion paths with their frequencies
     */
    List<Map<String, Object>> getConversionPaths(
        Long companyId,
        LocalDate startDate,
        LocalDate endDate,
        int maxPathLength
    );

    /**
     * Device Performance Comparison
     * 
     * @param companyId Company ID
     * @param startDate Start date
     * @param endDate End date
     * @return Performance metrics by device type
     */
    Map<String, Object> getDevicePerformance(Long companyId, LocalDate startDate, LocalDate endDate);

    /**
     * Promotion Performance Comparison
     * 
     * @param companyId Company ID
     * @param startDate Start date
     * @param endDate End date
     * @return Comparative metrics for all promotions
     */
    List<Map<String, Object>> getPromotionComparison(Long companyId, LocalDate startDate, LocalDate endDate);

    /**
     * Real-time Dashboard Metrics
     * 
     * @param companyId Company ID
     * @return Current real-time metrics (today's performance)
     */
    Map<String, Object> getRealTimeDashboard(Long companyId);

    /**
     * Export analytics data to CSV format
     * 
     * @param companyId Company ID
     * @param reportType Type of report (IMPRESSIONS, CONVERSIONS, REVENUE, etc.)
     * @param startDate Start date
     * @param endDate End date
     * @return CSV data as byte array
     */
    byte[] exportAnalyticsToCsv(Long companyId, String reportType, LocalDate startDate, LocalDate endDate);
}
