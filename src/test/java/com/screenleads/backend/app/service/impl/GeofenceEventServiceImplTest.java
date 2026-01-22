package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repository.GeofenceEventRepository;
import com.screenleads.backend.app.domain.repositories.DeviceRepository;
import com.screenleads.backend.app.service.GeofenceEventService;
import com.screenleads.backend.app.service.GeofenceRuleService;
import com.screenleads.backend.app.service.GeofenceZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeofenceEventServiceImplTest {

    @Mock
    private GeofenceEventRepository geofenceEventRepository;

    @Mock
    private GeofenceZoneService geofenceZoneService;

    @Mock
    private GeofenceRuleService geofenceRuleService;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private GeofenceEventServiceImpl geofenceEventService;

    private Company company;
    private Device device;
    private GeofenceZone zone;
    private GeofenceEvent event;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .build();

        device = Device.builder()
                .id(100L)
                .uuid("DEVICE-001")
                .company(company)
                .build();

        zone = GeofenceZone.builder()
                .id(200L)
                .name("Test Zone")
                .type(GeofenceType.CIRCLE)
                .geometry(Map.of("center_lat", 40.7128, "center_lng", -74.0060, "radius", 1000.0))
                .isActive(true)
                .build();

        event = GeofenceEvent.builder()
                .id(1L)
                .device(device)
                .zone(zone)
                .eventType(GeofenceEventType.ENTER)
                .timestamp(now)
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();
    }

    @Test
    void recordEvent_ShouldCreateNewEvent() {
        // Given
        GeofenceEvent newEvent = GeofenceEvent.builder()
                .device(device)
                .zone(zone)
                .eventType(GeofenceEventType.ENTER)
                .timestamp(now)
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        when(geofenceEventRepository.save(any(GeofenceEvent.class))).thenReturn(event);

        // When
        GeofenceEvent result = geofenceEventService.recordEvent(newEvent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDevice().getId()).isEqualTo(100L);
        assertThat(result.getZone().getId()).isEqualTo(200L);
        assertThat(result.getEventType()).isEqualTo(GeofenceEventType.ENTER);
        assertThat(result.getLatitude()).isEqualTo(40.7128);
        assertThat(result.getLongitude()).isEqualTo(-74.0060);

        verify(geofenceEventRepository, times(1)).save(any(GeofenceEvent.class));
    }

    @Test
    void recordEvent_WhenNoTimestamp_ShouldSetCurrentTime() {
        // Given
        GeofenceEvent newEvent = GeofenceEvent.builder()
                .device(device)
                .zone(zone)
                .eventType(GeofenceEventType.ENTER)
                .timestamp(null) // No timestamp provided
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        when(geofenceEventRepository.save(any(GeofenceEvent.class))).thenAnswer(invocation -> {
            GeofenceEvent savedEvent = invocation.getArgument(0);
            assertThat(savedEvent.getTimestamp()).isNotNull();
            return event;
        });

        // When
        GeofenceEvent result = geofenceEventService.recordEvent(newEvent);

        // Then
        assertThat(result).isNotNull();
        verify(geofenceEventRepository, times(1)).save(newEvent);
    }

    @Test
    void recordEvent_ShouldHandleExitEvent() {
        // Given
        GeofenceEvent exitEvent = GeofenceEvent.builder()
                .device(device)
                .zone(zone)
                .eventType(GeofenceEventType.EXIT)
                .timestamp(now)
                .latitude(40.7200)
                .longitude(-74.0100)
                .build();

        GeofenceEvent savedEvent = GeofenceEvent.builder()
                .id(2L)
                .device(device)
                .zone(zone)
                .eventType(GeofenceEventType.EXIT)
                .timestamp(now)
                .latitude(40.7200)
                .longitude(-74.0100)
                .build();

        when(geofenceEventRepository.save(any(GeofenceEvent.class))).thenReturn(savedEvent);

        // When
        GeofenceEvent result = geofenceEventService.recordEvent(exitEvent);

        // Then
        assertThat(result.getEventType()).isEqualTo(GeofenceEventType.EXIT);
        assertThat(result.getLatitude()).isEqualTo(40.7200);
        assertThat(result.getLongitude()).isEqualTo(-74.0100);
    }

    @Test
    void processLocationUpdate_ShouldCreateEventsForContainingZones() {
        // Given
        double lat = 40.7128;
        double lng = -74.0060;

        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(geofenceZoneService.findZonesContainingPoint(1L, lat, lng))
                .thenReturn(Collections.singletonList(zone));
        when(geofenceRuleService.findApplicableRules(200L))
                .thenReturn(Collections.emptyList());
        when(geofenceEventRepository.save(any(GeofenceEvent.class))).thenReturn(event);

        // When
        List<GeofenceEvent> results = geofenceEventService.processLocationUpdate(100L, lat, lng);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEventType()).isEqualTo(GeofenceEventType.ENTER);
        assertThat(results.get(0).getLatitude()).isEqualTo(lat);
        assertThat(results.get(0).getLongitude()).isEqualTo(lng);

        verify(geofenceZoneService, times(1)).findZonesContainingPoint(1L, lat, lng);
        verify(geofenceRuleService, times(1)).findApplicableRules(200L);
        verify(geofenceEventRepository, times(1)).save(any(GeofenceEvent.class));
    }

    @Test
    void processLocationUpdate_WhenDeviceNotFound_ShouldThrowException() {
        // Given
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> geofenceEventService.processLocationUpdate(999L, 40.7128, -74.0060))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Device not found: 999");

        verify(geofenceZoneService, never()).findZonesContainingPoint(anyLong(), anyDouble(), anyDouble());
    }

    @Test
    void processLocationUpdate_WhenNoZonesContainPoint_ShouldReturnEmptyList() {
        // Given
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(geofenceZoneService.findZonesContainingPoint(anyLong(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        // When
        List<GeofenceEvent> results = geofenceEventService.processLocationUpdate(100L, 40.7128, -74.0060);

        // Then
        assertThat(results).isEmpty();
        verify(geofenceEventRepository, never()).save(any());
    }

    @Test
    void processLocationUpdate_WhenMultipleZones_ShouldCreateMultipleEvents() {
        // Given
        GeofenceZone zone2 = GeofenceZone.builder()
                .id(201L)
                .name("Zone 2")
                .type(GeofenceType.RECTANGLE)
                .build();

        GeofenceEvent event2 = GeofenceEvent.builder()
                .id(2L)
                .device(device)
                .zone(zone2)
                .eventType(GeofenceEventType.ENTER)
                .build();

        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(geofenceZoneService.findZonesContainingPoint(anyLong(), anyDouble(), anyDouble()))
                .thenReturn(Arrays.asList(zone, zone2));
        when(geofenceRuleService.findApplicableRules(anyLong()))
                .thenReturn(Collections.emptyList());
        when(geofenceEventRepository.save(any(GeofenceEvent.class)))
                .thenReturn(event, event2);

        // When
        List<GeofenceEvent> results = geofenceEventService.processLocationUpdate(100L, 40.7128, -74.0060);

        // Then
        assertThat(results).hasSize(2);
        verify(geofenceEventRepository, times(2)).save(any(GeofenceEvent.class));
        verify(geofenceRuleService, times(1)).findApplicableRules(200L);
        verify(geofenceRuleService, times(1)).findApplicableRules(201L);
    }

    @Test
    void getEventById_WhenFound_ShouldReturnEvent() {
        // Given
        when(geofenceEventRepository.findById(1L)).thenReturn(Optional.of(event));

        // When
        Optional<GeofenceEvent> result = geofenceEventService.getEventById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getEventType()).isEqualTo(GeofenceEventType.ENTER);

        verify(geofenceEventRepository, times(1)).findById(1L);
    }

    @Test
    void getEventById_WhenNotFound_ShouldReturnEmpty() {
        // Given
        when(geofenceEventRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<GeofenceEvent> result = geofenceEventService.getEventById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getEventsByZone_ShouldReturnAllEventsForZone() {
        // Given
        GeofenceEvent event2 = GeofenceEvent.builder()
                .id(2L)
                .device(device)
                .zone(zone)
                .eventType(GeofenceEventType.EXIT)
                .timestamp(now.minusHours(1))
                .build();

        when(geofenceEventRepository.findByZone_IdOrderByCreatedAtDesc(200L))
                .thenReturn(Arrays.asList(event, event2));

        // When
        List<GeofenceEvent> results = geofenceEventService.getEventsByZone(200L);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getZone().getId()).isEqualTo(200L);
        assertThat(results.get(1).getZone().getId()).isEqualTo(200L);
    }

    @Test
    void getEventsByDevice_ShouldReturnAllEventsForDevice() {
        // Given
        GeofenceEvent event2 = GeofenceEvent.builder()
                .id(2L)
                .device(device)
                .zone(zone)
                .eventType(GeofenceEventType.DWELL)
                .timestamp(now.minusMinutes(30))
                .build();

        when(geofenceEventRepository.findByDevice_IdOrderByCreatedAtDesc(100L))
                .thenReturn(Arrays.asList(event, event2));

        // When
        List<GeofenceEvent> results = geofenceEventService.getEventsByDevice(100L);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getDevice().getId()).isEqualTo(100L);
        assertThat(results.get(1).getDevice().getId()).isEqualTo(100L);
    }

    @Test
    void getRecentEventsByZone_ShouldReturnLast24Hours() {
        // Given
        LocalDateTime yesterday = now.minusHours(24);

        when(geofenceEventRepository.findRecentEventsByZone(eq(200L), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(event));

        // When
        List<GeofenceEvent> results = geofenceEventService.getRecentEventsByZone(200L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getZone().getId()).isEqualTo(200L);

        verify(geofenceEventRepository, times(1))
                .findRecentEventsByZone(eq(200L), any(LocalDateTime.class));
    }

    @Test
    void getEventsByTypeAndDateRange_ShouldFilterByTypeAndDates() {
        // Given
        LocalDateTime start = now.minusDays(7);
        LocalDateTime end = now;

        when(geofenceEventRepository.findByEventTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                GeofenceEventType.ENTER, start, end))
                .thenReturn(Collections.singletonList(event));

        // When
        List<GeofenceEvent> results = geofenceEventService.getEventsByTypeAndDateRange(
                GeofenceEventType.ENTER, start, end);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEventType()).isEqualTo(GeofenceEventType.ENTER);

        verify(geofenceEventRepository, times(1))
                .findByEventTypeAndCreatedAtBetweenOrderByCreatedAtDesc(GeofenceEventType.ENTER, start, end);
    }

    @Test
    void getEventsByTypeAndDateRange_WhenNoEvents_ShouldReturnEmpty() {
        // Given
        LocalDateTime start = now.minusDays(7);
        LocalDateTime end = now;

        when(geofenceEventRepository.findByEventTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        List<GeofenceEvent> results = geofenceEventService.getEventsByTypeAndDateRange(
                GeofenceEventType.DWELL, start, end);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void countEventsByZoneAndType_ShouldReturnCount() {
        // Given
        when(geofenceEventRepository.countByZone_IdAndEventType(200L, GeofenceEventType.ENTER))
                .thenReturn(15L);

        // When
        Long result = geofenceEventService.countEventsByZoneAndType(200L, GeofenceEventType.ENTER);

        // Then
        assertThat(result).isEqualTo(15L);
        verify(geofenceEventRepository, times(1))
                .countByZone_IdAndEventType(200L, GeofenceEventType.ENTER);
    }

    @Test
    void countEventsByZoneAndType_WhenNoEvents_ShouldReturnZero() {
        // Given
        when(geofenceEventRepository.countByZone_IdAndEventType(200L, GeofenceEventType.EXIT))
                .thenReturn(0L);

        // When
        Long result = geofenceEventService.countEventsByZoneAndType(200L, GeofenceEventType.EXIT);

        // Then
        assertThat(result).isEqualTo(0L);
    }
}
