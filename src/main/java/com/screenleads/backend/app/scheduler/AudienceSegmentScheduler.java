package com.screenleads.backend.app.scheduler;

import com.screenleads.backend.app.domain.model.AudienceSegment;
import com.screenleads.backend.app.domain.repository.AudienceSegmentRepository;
import com.screenleads.backend.app.service.AudienceSegmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduler for automatic audience segment updates
 */
@Component
@Slf4j
public class AudienceSegmentScheduler {

    @Autowired
    private AudienceSegmentRepository audienceSegmentRepository;

    @Autowired
    private AudienceSegmentService audienceSegmentService;

    /**
     * Automatically rebuild audience segments that have auto-update enabled.
     * Runs every day at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void rebuildAutoUpdateSegments() {
        log.info("Starting scheduled rebuild of audience segments with auto-update enabled");

        try {
            // Find all segments with auto-update enabled
            List<AudienceSegment> autoUpdateSegments = audienceSegmentRepository.findByIsAutoUpdateTrue();

            if (autoUpdateSegments.isEmpty()) {
                log.info("No segments with auto-update enabled found");
                return;
            }

            log.info("Found {} segments with auto-update enabled", autoUpdateSegments.size());

            int successCount = 0;
            int failureCount = 0;

            // Rebuild each segment
            for (AudienceSegment segment : autoUpdateSegments) {
                try {
                    log.debug("Rebuilding segment: {} (ID: {})", segment.getName(), segment.getId());
                    audienceSegmentService.rebuildSegment(segment.getId());
                    successCount++;
                    log.info("Successfully rebuilt segment: {} with {} members",
                            segment.getName(), segment.getCustomerCount());
                } catch (Exception e) {
                    failureCount++;
                    log.error("Error rebuilding segment {} (ID: {}): {}",
                            segment.getName(), segment.getId(), e.getMessage(), e);
                }
            }

            log.info("Completed scheduled rebuild: {} successful, {} failed out of {} segments",
                    successCount, failureCount, autoUpdateSegments.size());

        } catch (Exception e) {
            log.error("Critical error during scheduled audience segment rebuild: {}", e.getMessage(), e);
        }
    }

    /**
     * Rebuild segments that need recalculation based on time threshold.
     * Runs every hour at minute 30.
     */
    @Scheduled(cron = "0 30 * * * *")
    public void rebuildStaleSegments() {
        log.debug("Checking for segments needing recalculation");

        try {
            List<AudienceSegment> staleSegments = audienceSegmentRepository.findSegmentsNeedingRecalculation();

            if (staleSegments.isEmpty()) {
                log.debug("No stale segments found");
                return;
            }

            log.info("Found {} stale segments requiring recalculation", staleSegments.size());

            for (AudienceSegment segment : staleSegments) {
                try {
                    audienceSegmentService.rebuildSegment(segment.getId());
                    log.info("Recalculated stale segment: {}", segment.getName());
                } catch (Exception e) {
                    log.error("Error recalculating segment {}: {}", segment.getName(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error during stale segments recalculation: {}", e.getMessage(), e);
        }
    }
}
