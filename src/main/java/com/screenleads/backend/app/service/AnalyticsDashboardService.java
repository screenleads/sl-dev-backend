package com.screenleads.backend.app.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Service interface for advanced analytics dashboard KPIs
 */
public interface AnalyticsDashboardService {

    /**
     * Get top 10 promotions by conversion rate within date range
     */
    List<TopPromotionDTO> getTopPromotionsByConversion(Long companyId, LocalDate startDate, LocalDate endDate,
            int limit);

    /**
     * Get top 10 promotions by total revenue within date range
     */
    List<TopPromotionDTO> getTopPromotionsByRevenue(Long companyId, LocalDate startDate, LocalDate endDate, int limit);

    /**
     * Calculate ROI for each promotion (Revenue vs Cost)
     */
    List<PromotionROI> getPromotionROI(Long companyId, LocalDate startDate, LocalDate endDate);

    /**
     * Get device performance metrics (impressions, conversions by device type)
     */
    List<DevicePerformance> getDevicePerformance(Long companyId, LocalDate startDate, LocalDate endDate);

    /**
     * Get hourly distribution of impressions and conversions
     */
    Map<Integer, HourlyMetrics> getHourlyDistribution(Long companyId, LocalDate startDate, LocalDate endDate);

    /**
     * Get metrics by geographic zone (geofence zones)
     */
    List<GeographicMetrics> getMetricsByGeofenceZone(Long companyId, LocalDate startDate, LocalDate endDate);

    /**
     * Get daily trends for key metrics (impressions, conversions, revenue)
     */
    List<DailyTrendDTO> getDailyTrends(Long companyId, LocalDate startDate, LocalDate endDate);

    /**
     * Get overall company dashboard summary
     */
    DashboardSummary getDashboardSummary(Long companyId, LocalDate startDate, LocalDate endDate);

    // DTO Classes

    interface TopPromotionDTO {
        Long getAdviceId();

        String getPromotionName();

        Long getTotalImpressions();

        Long getTotalConversions();

        Double getConversionRate();

        Double getTotalRevenue();
    }

    interface PromotionROI {
        Long getAdviceId();

        String getPromotionName();

        Double getTotalRevenue();

        Double getTotalCost(); // Could be calculated from pricing or fixed values

        Double getROI(); // (Revenue - Cost) / Cost * 100

        Long getTotalConversions();
    }

    interface DevicePerformance {
        Long getDeviceId();

        String getDeviceType();

        Long getTotalImpressions();

        Long getTotalConversions();

        Double getConversionRate();

        Double getAverageViewDuration();
    }

    interface HourlyMetrics {
        Integer getHour(); // 0-23

        Long getImpressions();

        Long getConversions();

        Double getConversionRate();
    }

    interface GeographicMetrics {
        Long getZoneId();

        String getZoneName();

        Long getTotalImpressions();

        Long getTotalConversions();

        Double getConversionRate();

        Integer getUniqueDevices();
    }

    interface DailyTrendDTO {
        LocalDate getDate();

        Long getImpressions();

        Long getConversions();

        Double getRevenue();

        Double getConversionRate();
    }

    interface DashboardSummary {
        Long getTotalImpressions();

        Long getTotalConversions();

        Double getOverallConversionRate();

        Double getTotalRevenue();

        Integer getActivePromotions();

        Integer getActiveDevices();

        Double getAverageOrderValue();

        LocalDate getPeriodStart();

        LocalDate getPeriodEnd();
    }
}
