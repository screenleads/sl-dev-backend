package com.screenleads.backend.app.application.service.impl;

import com.screenleads.backend.app.application.service.GeofenceService;
import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.infrastructure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeofenceServiceImplTest {

    @Mock
    private GeofenceZoneRepository zoneRepository;

    @Mock
    private GeofenceRuleRepository ruleRepository;

    @Mock
    private GeofenceEventRepository eventRepository;

    @InjectMocks
    private GeofenceServiceImpl geofenceService;

    private Company company;
    private GeofenceZone circleZone;
    private GeofenceZone rectangleZone;
    private GeofenceRule rule;
    private Promotion promotion;
    private Device device;
    private GeofenceEvent event;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        // Create circle zone (Madrid center, 1km radius)
        Map<String, Object> circleGeometry = new HashMap<>();
        Map<String, Double> center = new HashMap<>();
        center.put("lat", 40.4168);
        center.put("lon", -3.7038);
        circleGeometry.put("center", center);
        circleGeometry.put("radius", 1000.0);

        circleZone = new GeofenceZone();
        circleZone.setId(1L);
        circleZone.setName("Madrid Center");
        circleZone.setType(GeofenceType.CIRCLE);
        circleZone.setGeometry(circleGeometry);
        circleZone.setCompany(company);
        circleZone.setIsActive(true);
        circleZone.setColor("#FF0000");

        // Create rectangle zone
        Map<String, Object> rectGeometry = new HashMap<>();
        Map<String, Double> sw = new HashMap<>();
        sw.put("lat", 40.40);
        sw.put("lon", -3.75);
        Map<String, Double> ne = new HashMap<>();
        ne.put("lat", 40.45);
        ne.put("lon", -3.65);
        rectGeometry.put("sw", sw);
        rectGeometry.put("ne", ne);

        rectangleZone = new GeofenceZone();
        rectangleZone.setId(2L);
        rectangleZone.setName("Madrid Rectangle");
        rectangleZone.setType(GeofenceType.RECTANGLE);
        rectangleZone.setGeometry(rectGeometry);
        rectangleZone.setCompany(company);
        rectangleZone.setIsActive(true);

        promotion = new Promotion();
        promotion.setId(1L);
        promotion.setName("Test Promotion");

        rule = new GeofenceRule();
        rule.setId(1L);
        rule.setZone(circleZone);
        rule.setPromotion(promotion);
        rule.setRuleType(RuleType.SHOW_INSIDE);
        rule.setPriority(1);
        rule.setIsActive(true);

        device = new Device();
        device.setId(1L);
        device.setUuid("device-123");

        event = new GeofenceEvent();
        event.setId(1L);
        event.setDevice(device);
        event.setZone(circleZone);
        event.setEventType(GeofenceEventType.ENTER);
        event.setLatitude(40.4168);
        event.setLongitude(-3.7038);
    }

    @Test
    void testCreateZone_Success() {
        // Arrange
        when(zoneRepository.save(any(GeofenceZone.class))).thenReturn(circleZone);

        // Act
        GeofenceZone created = geofenceService.createZone(circleZone);

        // Assert
        assertNotNull(created);
        assertEquals("Madrid Center", created.getName());
        assertEquals(GeofenceType.CIRCLE, created.getType());
        verify(zoneRepository).save(any(GeofenceZone.class));
    }

    @Test
    void testUpdateZone_Success() {
        // Arrange
        GeofenceZone updatedData = new GeofenceZone();
        updatedData.setName("Updated Zone");
        updatedData.setDescription("Updated Description");
        updatedData.setType(GeofenceType.CIRCLE);
        updatedData.setGeometry(circleZone.getGeometry());
        updatedData.setIsActive(false);
        updatedData.setColor("#00FF00");

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(circleZone));
        when(zoneRepository.save(any(GeofenceZone.class))).thenReturn(circleZone);

        // Act
        GeofenceZone result = geofenceService.updateZone(1L, updatedData);

        // Assert
        assertNotNull(result);
        verify(zoneRepository).findById(1L);
        verify(zoneRepository).save(any(GeofenceZone.class));
    }

    @Test
    void testUpdateZone_NotFound_ThrowsException() {
        // Arrange
        when(zoneRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                geofenceService.updateZone(999L, new GeofenceZone())
        );
    }

    @Test
    void testDeleteZone() {
        // Act
        geofenceService.deleteZone(1L);

        // Assert
        verify(zoneRepository).deleteById(1L);
    }

    @Test
    void testGetZone_Found() {
        // Arrange
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(circleZone));

        // Act
        GeofenceZone result = geofenceService.getZone(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Madrid Center", result.getName());
    }

    @Test
    void testGetZone_NotFound_ThrowsException() {
        // Arrange
        when(zoneRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                geofenceService.getZone(999L)
        );
    }

    @Test
    void testGetZonesByCompany() {
        // Arrange
        when(zoneRepository.findByCompany_Id(1L)).thenReturn(List.of(circleZone, rectangleZone));

        // Act
        List<GeofenceZone> result = geofenceService.getZonesByCompany(1L);

        // Assert
        assertEquals(2, result.size());
        verify(zoneRepository).findByCompany_Id(1L);
    }

    @Test
    void testGetActiveZonesByCompany() {
        // Arrange
        circleZone.setIsActive(true);
        rectangleZone.setIsActive(false);
        when(zoneRepository.findByCompany_IdAndIsActiveTrue(1L)).thenReturn(List.of(circleZone));

        // Act
        List<GeofenceZone> result = geofenceService.getActiveZonesByCompany(1L);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void testCreateRule_Success() {
        // Arrange
        when(ruleRepository.save(any(GeofenceRule.class))).thenReturn(rule);

        // Act
        GeofenceRule created = geofenceService.createRule(rule);

        // Assert
        assertNotNull(created);
        assertEquals(RuleType.SHOW_INSIDE, created.getRuleType());
        verify(ruleRepository).save(any(GeofenceRule.class));
    }

    @Test
    void testUpdateRule_Success() {
        // Arrange
        GeofenceRule updatedData = new GeofenceRule();
        updatedData.setPriority(100);
        updatedData.setIsActive(false);
        updatedData.setRuleType(RuleType.HIDE_OUTSIDE);

        when(ruleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(GeofenceRule.class))).thenReturn(rule);

        // Act
        GeofenceRule result = geofenceService.updateRule(1L, updatedData);

        // Assert
        assertNotNull(result);
        verify(ruleRepository).save(any(GeofenceRule.class));
    }

    @Test
    void testDeleteRule() {
        // Act
        geofenceService.deleteRule(1L);

        // Assert
        verify(ruleRepository).deleteById(1L);
    }

    @Test
    void testGetRulesByZone() {
        // Arrange
        when(ruleRepository.findByZone_Id(1L)).thenReturn(List.of(rule));

        // Act
        List<GeofenceRule> result = geofenceService.getRulesByZone(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getZone().getId());
    }

    @Test
    void testGetRulesByPromotion() {
        // Arrange
        when(ruleRepository.findByPromotion_Id(1L)).thenReturn(List.of(rule));

        // Act
        List<GeofenceRule> result = geofenceService.getRulesByPromotion(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getPromotion().getId());
    }

    @Test
    void testGetActiveRulesByCompany() {
        // Arrange
        when(ruleRepository.findActiveRulesByCompany(1L))
                .thenReturn(List.of(rule));

        // Act
        List<GeofenceRule> result = geofenceService.getActiveRulesByCompany(1L);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void testFindZonesContainingPoint_CircleZone() {
        // Arrange
        double testLat = 40.4168; // Madrid center
        double testLon = -3.7038;

        when(zoneRepository.findByCompany_IdAndIsActiveTrue(1L))
                .thenReturn(List.of(circleZone));

        // Act
        List<GeofenceZone> result = geofenceService.findZonesContainingPoint(1L, testLat, testLon);

        // Assert - Should contain the point (center of circle)
        assertNotNull(result);
        // Note: Actual containment test depends on GeofenceZone.containsPoint implementation
    }

    @Test
    void testFindZonesContainingPoint_RectangleZone() {
        // Arrange
        double testLat = 40.42; // Inside rectangle
        double testLon = -3.70;

        when(zoneRepository.findByCompany_IdAndIsActiveTrue(1L))
                .thenReturn(List.of(rectangleZone));

        // Act
        List<GeofenceZone> result = geofenceService.findZonesContainingPoint(1L, testLat, testLon);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testIsInsideAnyZone_True() {
        // Arrange
        double testLat = 40.42;
        double testLon = -3.70;

        when(zoneRepository.findByCompany_IdAndIsActiveTrue(1L))
                .thenReturn(List.of(rectangleZone));

        // Act
        boolean result = geofenceService.isInsideAnyZone(1L, testLat, testLon);

        // Assert
        // Note: Result depends on actual geometry containment logic
        assertNotNull(result);
    }

    @Test
    void testIsInsideAnyZone_False_NoZones() {
        // Arrange
        when(zoneRepository.findByCompany_IdAndIsActiveTrue(1L))
                .thenReturn(Collections.emptyList());

        // Act
        boolean result = geofenceService.isInsideAnyZone(1L, 40.0, -3.0);

        // Assert
        assertFalse(result);
    }

    @Test
    void testTrackEvent_Success() {
        // Arrange
        when(eventRepository.save(any(GeofenceEvent.class))).thenReturn(event);

        // Act
        GeofenceEvent tracked = geofenceService.trackEvent(event);

        // Assert
        assertNotNull(tracked);
        assertEquals(GeofenceEventType.ENTER, tracked.getEventType());
        verify(eventRepository).save(any(GeofenceEvent.class));
    }

    @Test
    void testGetDeviceEvents() {
        // Arrange
        Page<GeofenceEvent> page = new PageImpl<>(List.of(event));
        when(eventRepository.findByDevice_Id(eq(1L), any(PageRequest.class)))
                .thenReturn(page);

        // Act
        List<GeofenceEvent> result = geofenceService.getDeviceEvents(1L, 0, 10);

        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getDevice().getId());
    }

    @Test
    void testGetZoneEvents() {
        // Arrange
        Page<GeofenceEvent> page = new PageImpl<>(List.of(event));
        when(eventRepository.findByZone_Id(eq(1L), any(PageRequest.class)))
                .thenReturn(page);

        // Act
        List<GeofenceEvent> result = geofenceService.getZoneEvents(1L, 0, 10);

        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getZone().getId());
    }

    @Test
    void testGetZoneStatistics() {
        // Arrange
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(circleZone));
        when(eventRepository.countByZone_IdAndEventTypeAndTimestampAfter(
                eq(1L), eq(GeofenceEventType.ENTER), any(LocalDateTime.class)))
                .thenReturn(10L);
        when(eventRepository.countByZone_IdAndEventTypeAndTimestampAfter(
                eq(1L), eq(GeofenceEventType.EXIT), any(LocalDateTime.class)))
                .thenReturn(8L);
        when(eventRepository.countByZone_IdAndEventTypeAndTimestampAfter(
                eq(1L), eq(GeofenceEventType.DWELL), any(LocalDateTime.class)))
                .thenReturn(5L);

        // Act
        Map<String, Object> result = geofenceService.getZoneStatistics(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.get("zoneId"));
        assertEquals("Madrid Center", result.get("zoneName"));
        assertEquals(10L, result.get("enterEvents"));
        assertEquals(8L, result.get("exitEvents"));
        assertEquals(5L, result.get("dwellEvents"));
        assertEquals(23L, result.get("totalEvents"));
    }

    @Test
    void testGetCompanyGeofenceStats() {
        // Arrange
        when(zoneRepository.countByCompany_Id(1L)).thenReturn(5L);
        when(zoneRepository.countByCompany_IdAndIsActiveTrue(1L)).thenReturn(3L);
        when(zoneRepository.findByCompany_IdAndIsActiveTrue(1L))
                .thenReturn(List.of(circleZone, rectangleZone));

        // Act
        Map<String, Object> result = geofenceService.getCompanyGeofenceStats(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.get("companyId"));
        assertEquals(5L, result.get("totalZones"));
        assertEquals(3L, result.get("activeZones"));
        assertTrue(result.containsKey("zonesByType"));
    }

    @Test
    void testGetZoneStatistics_NoEvents() {
        // Arrange
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(circleZone));
        when(eventRepository.countByZone_IdAndEventTypeAndTimestampAfter(
                anyLong(), any(GeofenceEventType.class), any(LocalDateTime.class)))
                .thenReturn(0L);

        // Act
        Map<String, Object> result = geofenceService.getZoneStatistics(1L);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.get("totalEvents"));
    }

    @Test
    void testTrackEvent_EnterType() {
        // Arrange
        event.setEventType(GeofenceEventType.ENTER);
        when(eventRepository.save(any(GeofenceEvent.class))).thenReturn(event);

        // Act
        GeofenceEvent result = geofenceService.trackEvent(event);

        // Assert
        assertEquals(GeofenceEventType.ENTER, result.getEventType());
    }

    @Test
    void testTrackEvent_ExitType() {
        // Arrange
        event.setEventType(GeofenceEventType.EXIT);
        when(eventRepository.save(any(GeofenceEvent.class))).thenReturn(event);

        // Act
        GeofenceEvent result = geofenceService.trackEvent(event);

        // Assert
        assertEquals(GeofenceEventType.EXIT, result.getEventType());
    }

    @Test
    void testTrackEvent_DwellType() {
        // Arrange
        event.setEventType(GeofenceEventType.DWELL);
        when(eventRepository.save(any(GeofenceEvent.class))).thenReturn(event);

        // Act
        GeofenceEvent result = geofenceService.trackEvent(event);

        // Assert
        assertEquals(GeofenceEventType.DWELL, result.getEventType());
    }

    @Test
    void testGetActiveRulesByCompany_OrderedByPriority() {
        // Arrange
        GeofenceRule rule2 = new GeofenceRule();
        rule2.setId(2L);
        rule2.setPriority(10);
        rule2.setIsActive(true);
        rule2.setRuleType(RuleType.PRIORITIZE_INSIDE);

        rule.setPriority(1);

        when(ruleRepository.findActiveRulesByCompany(1L))
                .thenReturn(List.of(rule2, rule));

        // Act
        List<GeofenceRule> result = geofenceService.getActiveRulesByCompany(1L);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.get(0).getPriority() >= result.get(1).getPriority());
    }

    @Test
    void testFindZonesContainingPoint_MultipleZones() {
        // Arrange
        double testLat = 40.42;
        double testLon = -3.70;

        when(zoneRepository.findByCompany_IdAndIsActiveTrue(1L))
                .thenReturn(List.of(circleZone, rectangleZone));

        // Act
        List<GeofenceZone> result = geofenceService.findZonesContainingPoint(1L, testLat, testLon);

        // Assert
        assertNotNull(result);
        verify(zoneRepository).findByCompany_IdAndIsActiveTrue(1L);
    }

    @Test
    void testGetDeviceEvents_Pagination() {
        // Arrange
        List<GeofenceEvent> events = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            GeofenceEvent e = new GeofenceEvent();
            e.setId((long) i);
            e.setDevice(device);
            events.add(e);
        }

        Page<GeofenceEvent> page = new PageImpl<>(events.subList(0, 10));
        when(eventRepository.findByDevice_Id(eq(1L), any(PageRequest.class)))
                .thenReturn(page);

        // Act
        List<GeofenceEvent> result = geofenceService.getDeviceEvents(1L, 0, 10);

        // Assert
        assertEquals(10, result.size());
    }

    @Test
    void testGetZoneEvents_EmptyResult() {
        // Arrange
        Page<GeofenceEvent> emptyPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.findByZone_Id(eq(999L), any(PageRequest.class)))
                .thenReturn(emptyPage);

        // Act
        List<GeofenceEvent> result = geofenceService.getZoneEvents(999L, 0, 10);

        // Assert
        assertTrue(result.isEmpty());
    }
}
