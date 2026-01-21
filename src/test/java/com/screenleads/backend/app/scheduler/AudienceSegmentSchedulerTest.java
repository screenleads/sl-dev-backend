package com.screenleads.backend.app.scheduler;

import com.screenleads.backend.app.domain.model.AudienceSegment;
import com.screenleads.backend.app.domain.repository.AudienceSegmentRepository;
import com.screenleads.backend.app.service.AudienceSegmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudienceSegmentSchedulerTest {

    @Mock
    private AudienceSegmentRepository audienceSegmentRepository;

    @Mock
    private AudienceSegmentService audienceSegmentService;

    @InjectMocks
    private AudienceSegmentScheduler scheduler;

    private AudienceSegment segment1;
    private AudienceSegment segment2;

    @BeforeEach
    void setUp() {
        segment1 = new AudienceSegment();
        segment1.setId(1L);
        segment1.setName("Active Customers");
        segment1.setIsAutoUpdate(true);
        segment1.setCustomerCount(100L);

        segment2 = new AudienceSegment();
        segment2.setId(2L);
        segment2.setName("Inactive Customers");
        segment2.setIsAutoUpdate(true);
        segment2.setCustomerCount(50L);
    }

    @Test
    void testRebuildAutoUpdateSegments_WithSegments() {
        // Given
        List<AudienceSegment> segments = Arrays.asList(segment1, segment2);
        when(audienceSegmentRepository.findByIsAutoUpdateTrue()).thenReturn(segments);
        doNothing().when(audienceSegmentService).rebuildSegment(anyLong());

        // When
        scheduler.rebuildAutoUpdateSegments();

        // Then
        verify(audienceSegmentRepository, times(1)).findByIsAutoUpdateTrue();
        verify(audienceSegmentService, times(1)).rebuildSegment(1L);
        verify(audienceSegmentService, times(1)).rebuildSegment(2L);
    }

    @Test
    void testRebuildAutoUpdateSegments_NoSegments() {
        // Given
        when(audienceSegmentRepository.findByIsAutoUpdateTrue()).thenReturn(Collections.emptyList());

        // When
        scheduler.rebuildAutoUpdateSegments();

        // Then
        verify(audienceSegmentRepository, times(1)).findByIsAutoUpdateTrue();
        verify(audienceSegmentService, never()).rebuildSegment(anyLong());
    }

    @Test
    void testRebuildAutoUpdateSegments_WithFailure() {
        // Given
        List<AudienceSegment> segments = Arrays.asList(segment1, segment2);
        when(audienceSegmentRepository.findByIsAutoUpdateTrue()).thenReturn(segments);
        doThrow(new RuntimeException("Database error")).when(audienceSegmentService).rebuildSegment(1L);
        doNothing().when(audienceSegmentService).rebuildSegment(2L);

        // When
        scheduler.rebuildAutoUpdateSegments();

        // Then
        verify(audienceSegmentRepository, times(1)).findByIsAutoUpdateTrue();
        verify(audienceSegmentService, times(1)).rebuildSegment(1L);
        verify(audienceSegmentService, times(1)).rebuildSegment(2L); // Should continue despite failure
    }

    @Test
    void testRebuildStaleSegments_WithStaleSegments() {
        // Given
        segment1.setLastCalculatedAt(LocalDateTime.now().minusHours(2));
        List<AudienceSegment> staleSegments = Collections.singletonList(segment1);
        when(audienceSegmentRepository.findSegmentsNeedingRecalculation()).thenReturn(staleSegments);
        doNothing().when(audienceSegmentService).rebuildSegment(anyLong());

        // When
        scheduler.rebuildStaleSegments();

        // Then
        verify(audienceSegmentRepository, times(1)).findSegmentsNeedingRecalculation();
        verify(audienceSegmentService, times(1)).rebuildSegment(1L);
    }

    @Test
    void testRebuildStaleSegments_NoStaleSegments() {
        // Given
        when(audienceSegmentRepository.findSegmentsNeedingRecalculation()).thenReturn(Collections.emptyList());

        // When
        scheduler.rebuildStaleSegments();

        // Then
        verify(audienceSegmentRepository, times(1)).findSegmentsNeedingRecalculation();
        verify(audienceSegmentService, never()).rebuildSegment(anyLong());
    }

    @Test
    void testRebuildStaleSegments_WithException() {
        // Given
        segment1.setLastCalculatedAt(LocalDateTime.now().minusHours(2));
        List<AudienceSegment> staleSegments = Collections.singletonList(segment1);
        when(audienceSegmentRepository.findSegmentsNeedingRecalculation()).thenReturn(staleSegments);
        doThrow(new RuntimeException("Service error")).when(audienceSegmentService).rebuildSegment(1L);

        // When - should not throw, just log
        scheduler.rebuildStaleSegments();

        // Then
        verify(audienceSegmentRepository, times(1)).findSegmentsNeedingRecalculation();
        verify(audienceSegmentService, times(1)).rebuildSegment(1L);
    }
}
