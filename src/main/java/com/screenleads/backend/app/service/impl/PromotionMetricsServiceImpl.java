package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.PromotionMetrics;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repository.AdviceImpressionRepository;
import com.screenleads.backend.app.domain.repository.AdviceInteractionRepository;
import com.screenleads.backend.app.domain.repository.PromotionMetricsRepository;
import com.screenleads.backend.app.service.PromotionMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionMetricsServiceImpl implements PromotionMetricsService {

    private final PromotionMetricsRepository promotionMetricsRepository;
    private final AdviceRepository adviceRepository;
    private final AdviceImpressionRepository adviceImpressionRepository;
    private final AdviceInteractionRepository adviceInteractionRepository;

    @Override
    @Transactional
    public void calculateDailyMetrics(LocalDate date) {
        log.info("Starting daily metrics calculation for date: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // Get all active advice/promotions
        List<Advice> allAdvice = adviceRepository.findAll();
        log.info("Found {} active promotions to calculate metrics for", allAdvice.size());

        int calculatedCount = 0;
        for (Advice advice : allAdvice) {
            try {
                calculateMetricsForAdvice(advice, date, startOfDay, endOfDay);
                calculatedCount++;
            } catch (Exception e) {
                log.error("Error calculating metrics for advice ID {}: {}", advice.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed daily metrics calculation for date: {}. Calculated metrics for {} promotions.",
                date, calculatedCount);
    }

    @Transactional
    protected void calculateMetricsForAdvice(Advice advice, LocalDate date,
            LocalDateTime startOfDay, LocalDateTime endOfDay) {
        Long adviceId = advice.getId();

        // Count impressions for this day
        Long totalImpressions = adviceImpressionRepository.countByAdviceIdAndDateRange(
                adviceId, startOfDay, endOfDay);

        // Count interactions for this day
        Long totalInteractions = adviceInteractionRepository.countByAdviceIdAndDateRange(
                adviceId, startOfDay, endOfDay);

        // Count conversions for this day
        Long totalConversions = adviceInteractionRepository.countConversionsByAdviceIdAndDateRange(
                adviceId, startOfDay, endOfDay);

        // Count unique customers
        Long uniqueCustomers = adviceImpressionRepository.countUniqueCustomersByAdviceIdAndDateRange(
                adviceId, startOfDay, endOfDay);

        // Count unique devices
        Long uniqueDevices = adviceImpressionRepository.countUniqueDevicesByAdviceIdAndDateRange(
                adviceId, startOfDay, endOfDay);

        // Calculate average view duration
        Double avgViewDuration = adviceImpressionRepository.calculateAverageDurationByAdviceIdAndDateRange(
                adviceId, startOfDay, endOfDay);

        // Check if metrics already exist for this date
        Optional<PromotionMetrics> existingMetrics = promotionMetricsRepository.findByAdvice_IdAndMetricDate(adviceId,
                date);

        PromotionMetrics metrics;
        if (existingMetrics.isPresent()) {
            // Update existing metrics
            metrics = existingMetrics.get();
            log.debug("Updating existing metrics for advice ID {} on date {}", adviceId, date);
        } else {
            // Create new metrics
            metrics = PromotionMetrics.builder()
                    .advice(advice)
                    .metricDate(date)
                    .build();
            log.debug("Creating new metrics for advice ID {} on date {}", adviceId, date);
        }

        // Set all metric values
        metrics.setTotalImpressions(totalImpressions);
        metrics.setTotalInteractions(totalInteractions);
        metrics.setTotalConversions(totalConversions);
        metrics.setUniqueCustomers(uniqueCustomers);
        metrics.setUniqueDevices(uniqueDevices);
        metrics.setAvgViewDurationSeconds(avgViewDuration);

        // Calculate rates
        metrics.calculateConversionRate();
        metrics.calculateClickThroughRate();

        // Save metrics
        promotionMetricsRepository.save(metrics);

        log.debug("Saved metrics for advice ID {}: impressions={}, interactions={}, conversions={}, CR={}%",
                adviceId, totalImpressions, totalInteractions, totalConversions,
                metrics.getConversionRate());
    }

    @Override
    @Transactional
    public void calculateDailyMetricsForYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Calculating daily metrics for yesterday: {}", yesterday);
        calculateDailyMetrics(yesterday);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PromotionMetrics> getMetricsByAdviceAndDate(Long adviceId, LocalDate date) {
        return promotionMetricsRepository.findByAdvice_IdAndMetricDate(adviceId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionMetrics> getMetricsByAdvice(Long adviceId) {
        return promotionMetricsRepository.findByAdvice_IdOrderByMetricDateDesc(adviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionMetrics> getMetricsByAdviceAndDateRange(Long adviceId, LocalDate startDate,
            LocalDate endDate) {
        return promotionMetricsRepository.findByAdviceIdAndDateRange(adviceId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionMetrics> getMetricsByDate(LocalDate date) {
        return promotionMetricsRepository.findByMetricDateOrderByTotalImpressionsDesc(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionMetrics> getTopPerformingByConversionRate(LocalDate startDate, LocalDate endDate, int limit) {
        return promotionMetricsRepository.findTopPerformingByConversionRate(startDate, endDate)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionMetrics> getTopPerformingByConversions(LocalDate startDate, LocalDate endDate, int limit) {
        return promotionMetricsRepository.findTopPerformingByConversions(startDate, endDate)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalImpressionsByAdvice(Long adviceId) {
        return promotionMetricsRepository.getTotalImpressionsByAdviceId(adviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalConversionsByAdvice(Long adviceId) {
        return promotionMetricsRepository.getTotalConversionsByAdviceId(adviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean metricsExistForDate(LocalDate date) {
        return promotionMetricsRepository.existsByMetricDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> getLatestMetricDate() {
        return promotionMetricsRepository.findLatestMetricDate();
    }
}
