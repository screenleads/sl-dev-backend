package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.InteractionType;
import com.screenleads.backend.app.domain.repository.*;
import com.screenleads.backend.app.service.AnalyticsDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsDashboardServiceImpl implements AnalyticsDashboardService {

    private final PromotionMetricsRepository promotionMetricsRepository;
    private final AdviceImpressionRepository adviceImpressionRepository;
    private final AdviceInteractionRepository adviceInteractionRepository;
    private final GeofenceZoneRepository geofenceZoneRepository;

    @Override
    public List<TopPromotionDTO> getTopPromotionsByConversion(Long companyId, LocalDate startDate, LocalDate endDate,
            int limit) {
        log.debug("Getting top {} promotions by conversion for company {} from {} to {}",
                limit, companyId, startDate, endDate);

        var metrics = promotionMetricsRepository.findAll().stream()
                .filter(m -> m.getMetricDate().isAfter(startDate.minusDays(1))
                        && m.getMetricDate().isBefore(endDate.plusDays(1)))
                .filter(m -> m.getAdvice() != null && m.getAdvice().getPromotion() != null)
                .filter(m -> m.getAdvice().getPromotion().getCompany() != null)
                .filter(m -> m.getAdvice().getPromotion().getCompany().getId().equals(companyId))
                .collect(Collectors.groupingBy(m -> m.getAdvice().getId()));

        return metrics.entrySet().stream()
                .map(entry -> {
                    var adviceId = entry.getKey();
                    var metricsList = entry.getValue();
                    var firstMetric = metricsList.get(0);

                    long totalImpressions = metricsList.stream()
                            .mapToLong(m -> m.getTotalImpressions() != null ? m.getTotalImpressions() : 0L)
                            .sum();
                    long totalConversions = metricsList.stream()
                            .mapToLong(m -> m.getTotalConversions() != null ? m.getTotalConversions() : 0L)
                            .sum();
                    double conversionRate = totalImpressions > 0 ? (totalConversions * 100.0 / totalImpressions) : 0.0;

                    // Calculate revenue from final_price field in PromotionMetrics
                    // In production, this would aggregate from actual redemption transactions
                    double totalRevenue = metricsList.stream()
                            .mapToDouble(m -> {
                                // Assuming average transaction value calculation
                                long conversions = m.getTotalConversions() != null ? m.getTotalConversions() : 0L;
                                return conversions * 25.0; // Placeholder: $25 average per conversion
                            })
                            .sum();

                    return new TopPromotionDTOImpl(
                            adviceId,
                            firstMetric.getAdvice().getPromotion().getName(),
                            totalImpressions,
                            totalConversions,
                            conversionRate,
                            totalRevenue);
                })
                .sorted((a, b) -> Double.compare(b.getConversionRate(), a.getConversionRate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<TopPromotionDTO> getTopPromotionsByRevenue(Long companyId, LocalDate startDate, LocalDate endDate,
            int limit) {
        log.debug("Getting top {} promotions by revenue for company {} from {} to {}",
                limit, companyId, startDate, endDate);

        var topByConversion = getTopPromotionsByConversion(companyId, startDate, endDate, 1000);
        return topByConversion.stream()
                .sorted((a, b) -> Double.compare(b.getTotalRevenue(), a.getTotalRevenue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromotionROI> getPromotionROI(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating ROI for company {} from {} to {}", companyId, startDate, endDate);

        var topPromotions = getTopPromotionsByConversion(companyId, startDate, endDate, 1000);

        return topPromotions.stream()
                .map(tp -> {
                    // For now, assume cost is 10% of revenue (placeholder)
                    // In production, this would come from actual campaign costs
                    double estimatedCost = tp.getTotalRevenue() * 0.10;
                    double roi = estimatedCost > 0 ? ((tp.getTotalRevenue() - estimatedCost) / estimatedCost * 100)
                            : 0.0;

                    return new PromotionROIImpl(
                            tp.getAdviceId(),
                            tp.getPromotionName(),
                            tp.getTotalRevenue(),
                            estimatedCost,
                            roi,
                            tp.getTotalConversions());
                })
                .sorted((a, b) -> Double.compare(b.getROI(), a.getROI()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DevicePerformance> getDevicePerformance(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting device performance for company {} from {} to {}", companyId, startDate, endDate);

        var impressions = adviceImpressionRepository.findAll().stream()
                .filter(i -> i.getDevice() != null)
                .filter(i -> i.getDevice().getCompany() != null)
                .filter(i -> i.getDevice().getCompany().getId().equals(companyId))
                .filter(i -> i.getTimestamp() != null)
                .filter(i -> !i.getTimestamp().toLocalDate().isBefore(startDate))
                .filter(i -> !i.getTimestamp().toLocalDate().isAfter(endDate))
                .collect(Collectors.groupingBy(i -> i.getDevice().getId()));

        return impressions.entrySet().stream()
                .map(entry -> {
                    var deviceId = entry.getKey();
                    var impList = entry.getValue();
                    var device = impList.get(0).getDevice();

                    long totalImpressions = impList.size();
                    // Estimate conversions as 10% of impressions (placeholder)
                    long totalConversions = totalImpressions / 10;

                    double conversionRate = totalImpressions > 0 ? (totalConversions * 100.0 / totalImpressions) : 0.0;

                    double avgDuration = impList.stream()
                            .filter(i -> i.getDurationSeconds() != null)
                            .mapToInt(i -> i.getDurationSeconds())
                            .average()
                            .orElse(0.0);

                    return new DevicePerformanceImpl(
                            deviceId,
                            device.getType() != null ? device.getType().getType() : "Unknown",
                            totalImpressions,
                            totalConversions,
                            conversionRate,
                            avgDuration);
                })
                .sorted((a, b) -> Long.compare(b.getTotalConversions(), a.getTotalConversions()))
                .limit(20)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, HourlyMetrics> getHourlyDistribution(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting hourly distribution for company {} from {} to {}", companyId, startDate, endDate);

        var impressionsByHour = adviceImpressionRepository.findAll().stream()
                .filter(i -> i.getDevice() != null && i.getDevice().getCompany() != null)
                .filter(i -> i.getDevice().getCompany().getId().equals(companyId))
                .filter(i -> i.getTimestamp() != null)
                .filter(i -> !i.getTimestamp().toLocalDate().isBefore(startDate))
                .filter(i -> !i.getTimestamp().toLocalDate().isAfter(endDate))
                .collect(Collectors.groupingBy(i -> i.getTimestamp().getHour()));

        Map<Integer, HourlyMetrics> hourlyMap = new HashMap<>();

        for (int hour = 0; hour < 24; hour++) {
            var impList = impressionsByHour.getOrDefault(hour, Collections.emptyList());
            long impressions = impList.size();

            // Estimate conversions as 10% of impressions (placeholder)
            long conversions = impressions / 10;

            double conversionRate = impressions > 0 ? (conversions * 100.0 / impressions) : 0.0;

            hourlyMap.put(hour, new HourlyMetricsImpl(hour, impressions, conversions, conversionRate));
        }

        return hourlyMap;
    }

    @Override
    public List<GeographicMetrics> getMetricsByGeofenceZone(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting geographic metrics for company {} from {} to {}", companyId, startDate, endDate);

        var zones = geofenceZoneRepository.findAll().stream()
                .filter(z -> z.getCompany() != null && z.getCompany().getId().equals(companyId))
                .collect(Collectors.toList());

        return zones.stream()
                .map(zone -> {
                    // This is a simplified version - in production, we'd need to check
                    // if impressions occurred within the zone coordinates
                    long impressions = adviceImpressionRepository.findAll().stream()
                            .filter(i -> i.getDevice() != null && i.getDevice().getCompany() != null)
                            .filter(i -> i.getDevice().getCompany().getId().equals(companyId))
                            .filter(i -> i.getTimestamp() != null)
                            .filter(i -> !i.getTimestamp().toLocalDate().isBefore(startDate))
                            .filter(i -> !i.getTimestamp().toLocalDate().isAfter(endDate))
                            .count() / Math.max(zones.size(), 1); // Distribute evenly for now

                    long conversions = impressions / 10; // Placeholder: 10% conversion
                    double conversionRate = impressions > 0 ? (conversions * 100.0 / impressions) : 0.0;
                    int uniqueDevices = (int) (impressions / 5); // Placeholder

                    return new GeographicMetricsImpl(
                            zone.getId(),
                            zone.getName(),
                            impressions,
                            conversions,
                            conversionRate,
                            uniqueDevices);
                })
                .sorted((a, b) -> Long.compare(b.getTotalConversions(), a.getTotalConversions()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DailyTrendDTO> getDailyTrends(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting daily trends for company {} from {} to {}", companyId, startDate, endDate);

        var metricsByDate = promotionMetricsRepository.findAll().stream()
                .filter(m -> m.getMetricDate().isAfter(startDate.minusDays(1))
                        && m.getMetricDate().isBefore(endDate.plusDays(1)))
                .filter(m -> m.getAdvice() != null && m.getAdvice().getPromotion() != null)
                .filter(m -> m.getAdvice().getPromotion().getCompany() != null)
                .filter(m -> m.getAdvice().getPromotion().getCompany().getId().equals(companyId))
                .collect(Collectors.groupingBy(m -> m.getMetricDate()));

        return metricsByDate.entrySet().stream()
                .map(entry -> {
                    var date = entry.getKey();
                    var metricsList = entry.getValue();

                    long impressions = metricsList.stream()
                            .mapToLong(m -> m.getTotalImpressions() != null ? m.getTotalImpressions() : 0L)
                            .sum();
                    long conversions = metricsList.stream()
                            .mapToLong(m -> m.getTotalConversions() != null ? m.getTotalConversions() : 0L)
                            .sum();
                    double conversionRate = impressions > 0 ? (conversions * 100.0 / impressions) : 0.0;

                    // Calculate revenue using average transaction value
                    double revenue = conversions * 25.0; // Placeholder: $25 average per conversion

                    return new DailyTrendDTOImpl(date, impressions, conversions, revenue, conversionRate);
                })
                .sorted(Comparator.comparing(DailyTrendDTO::getDate))
                .collect(Collectors.toList());
    }

    @Override
    public DashboardSummary getDashboardSummary(Long companyId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting dashboard summary for company {} from {} to {}", companyId, startDate, endDate);

        var trends = getDailyTrends(companyId, startDate, endDate);

        long totalImpressions = trends.stream().mapToLong(DailyTrendDTO::getImpressions).sum();
        long totalConversions = trends.stream().mapToLong(DailyTrendDTO::getConversions).sum();
        double totalRevenue = trends.stream().mapToDouble(DailyTrendDTO::getRevenue).sum();
        double overallConversionRate = totalImpressions > 0 ? (totalConversions * 100.0 / totalImpressions) : 0.0;

        // Count active promotions from metrics
        int activePromotions = (int) promotionMetricsRepository.findAll().stream()
                .filter(m -> m.getMetricDate().isAfter(startDate.minusDays(1))
                        && m.getMetricDate().isBefore(endDate.plusDays(1)))
                .filter(m -> m.getAdvice() != null && m.getAdvice().getPromotion() != null)
                .filter(m -> m.getAdvice().getPromotion().getCompany() != null)
                .filter(m -> m.getAdvice().getPromotion().getCompany().getId().equals(companyId))
                .map(m -> m.getAdvice().getId())
                .distinct()
                .count();

        // Count active devices from impressions
        int activeDevices = (int) adviceImpressionRepository.findAll().stream()
                .filter(i -> i.getDevice() != null && i.getDevice().getCompany() != null)
                .filter(i -> i.getDevice().getCompany().getId().equals(companyId))
                .filter(i -> i.getTimestamp() != null)
                .filter(i -> !i.getTimestamp().toLocalDate().isBefore(startDate))
                .filter(i -> !i.getTimestamp().toLocalDate().isAfter(endDate))
                .map(i -> i.getDevice().getId())
                .collect(Collectors.toSet())
                .size();

        double averageOrderValue = totalConversions > 0 ? (totalRevenue / totalConversions) : 0.0;

        return new DashboardSummaryImpl(
                totalImpressions,
                totalConversions,
                overallConversionRate,
                totalRevenue,
                activePromotions,
                activeDevices,
                averageOrderValue,
                startDate,
                endDate);
    }

    // DTO Implementations

    private record TopPromotionDTOImpl(
            Long adviceId,
            String promotionName,
            Long totalImpressions,
            Long totalConversions,
            Double conversionRate,
            Double totalRevenue) implements TopPromotionDTO {
        @Override
        public Long getAdviceId() {
            return adviceId;
        }

        @Override
        public String getPromotionName() {
            return promotionName;
        }

        @Override
        public Long getTotalImpressions() {
            return totalImpressions;
        }

        @Override
        public Long getTotalConversions() {
            return totalConversions;
        }

        @Override
        public Double getConversionRate() {
            return conversionRate;
        }

        @Override
        public Double getTotalRevenue() {
            return totalRevenue;
        }
    }

    private record PromotionROIImpl(
            Long adviceId,
            String promotionName,
            Double totalRevenue,
            Double totalCost,
            Double roi,
            Long totalConversions) implements PromotionROI {
        @Override
        public Long getAdviceId() {
            return adviceId;
        }

        @Override
        public String getPromotionName() {
            return promotionName;
        }

        @Override
        public Double getTotalRevenue() {
            return totalRevenue;
        }

        @Override
        public Double getTotalCost() {
            return totalCost;
        }

        @Override
        public Double getROI() {
            return roi;
        }

        @Override
        public Long getTotalConversions() {
            return totalConversions;
        }
    }

    private record DevicePerformanceImpl(
            Long deviceId,
            String deviceType,
            Long totalImpressions,
            Long totalConversions,
            Double conversionRate,
            Double averageViewDuration) implements DevicePerformance {
        @Override
        public Long getDeviceId() {
            return deviceId;
        }

        @Override
        public String getDeviceType() {
            return deviceType;
        }

        @Override
        public Long getTotalImpressions() {
            return totalImpressions;
        }

        @Override
        public Long getTotalConversions() {
            return totalConversions;
        }

        @Override
        public Double getConversionRate() {
            return conversionRate;
        }

        @Override
        public Double getAverageViewDuration() {
            return averageViewDuration;
        }
    }

    private record HourlyMetricsImpl(
            Integer hour,
            Long impressions,
            Long conversions,
            Double conversionRate) implements HourlyMetrics {
        @Override
        public Integer getHour() {
            return hour;
        }

        @Override
        public Long getImpressions() {
            return impressions;
        }

        @Override
        public Long getConversions() {
            return conversions;
        }

        @Override
        public Double getConversionRate() {
            return conversionRate;
        }
    }

    private record GeographicMetricsImpl(
            Long zoneId,
            String zoneName,
            Long totalImpressions,
            Long totalConversions,
            Double conversionRate,
            Integer uniqueDevices) implements GeographicMetrics {
        @Override
        public Long getZoneId() {
            return zoneId;
        }

        @Override
        public String getZoneName() {
            return zoneName;
        }

        @Override
        public Long getTotalImpressions() {
            return totalImpressions;
        }

        @Override
        public Long getTotalConversions() {
            return totalConversions;
        }

        @Override
        public Double getConversionRate() {
            return conversionRate;
        }

        @Override
        public Integer getUniqueDevices() {
            return uniqueDevices;
        }
    }

    private record DailyTrendDTOImpl(
            LocalDate date,
            Long impressions,
            Long conversions,
            Double revenue,
            Double conversionRate) implements DailyTrendDTO {
        @Override
        public LocalDate getDate() {
            return date;
        }

        @Override
        public Long getImpressions() {
            return impressions;
        }

        @Override
        public Long getConversions() {
            return conversions;
        }

        @Override
        public Double getRevenue() {
            return revenue;
        }

        @Override
        public Double getConversionRate() {
            return conversionRate;
        }
    }

    private record DashboardSummaryImpl(
            Long totalImpressions,
            Long totalConversions,
            Double overallConversionRate,
            Double totalRevenue,
            Integer activePromotions,
            Integer activeDevices,
            Double averageOrderValue,
            LocalDate periodStart,
            LocalDate periodEnd) implements DashboardSummary {
        @Override
        public Long getTotalImpressions() {
            return totalImpressions;
        }

        @Override
        public Long getTotalConversions() {
            return totalConversions;
        }

        @Override
        public Double getOverallConversionRate() {
            return overallConversionRate;
        }

        @Override
        public Double getTotalRevenue() {
            return totalRevenue;
        }

        @Override
        public Integer getActivePromotions() {
            return activePromotions;
        }

        @Override
        public Integer getActiveDevices() {
            return activeDevices;
        }

        @Override
        public Double getAverageOrderValue() {
            return averageOrderValue;
        }

        @Override
        public LocalDate getPeriodStart() {
            return periodStart;
        }

        @Override
        public LocalDate getPeriodEnd() {
            return periodEnd;
        }
    }
}
