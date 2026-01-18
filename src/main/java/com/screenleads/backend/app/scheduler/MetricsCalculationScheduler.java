package com.screenleads.backend.app.scheduler;

import com.screenleads.backend.app.service.PromotionMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled job to calculate daily promotion metrics
 * Runs every night at 2:00 AM to calculate metrics for the previous day
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsCalculationScheduler {

    private final PromotionMetricsService promotionMetricsService;

    /**
     * Calculate daily metrics for yesterday
     * Scheduled to run every day at 2:00 AM
     * Cron: "0 0 2 * * ?" = At 02:00:00 every day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void calculateDailyMetrics() {
        log.info("=== Starting scheduled daily metrics calculation ===");
        try {
            promotionMetricsService.calculateDailyMetricsForYesterday();
            log.info("=== Completed scheduled daily metrics calculation successfully ===");
        } catch (Exception e) {
            log.error("=== Error during scheduled daily metrics calculation: {} ===", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for testing - calculates metrics for a specific date
     * This can be called via an admin endpoint if needed
     */
    public void calculateMetricsForDate(LocalDate date) {
        log.info("=== Starting manual metrics calculation for date: {} ===", date);
        try {
            promotionMetricsService.calculateDailyMetrics(date);
            log.info("=== Completed manual metrics calculation for date: {} ===", date);
        } catch (Exception e) {
            log.error("=== Error during manual metrics calculation for date {}: {} ===", 
                     date, e.getMessage(), e);
        }
    }
}
