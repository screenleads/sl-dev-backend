package com.screenleads.backend.app.service.impl;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.GeofenceZone;
import com.screenleads.backend.app.domain.model.GeofenceType;
import com.screenleads.backend.app.domain.repository.GeofenceZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeofenceZoneServiceImplTest {

    @Mock
    private GeofenceZoneRepository geofenceZoneRepository;

    @InjectMocks
    private GeofenceZoneServiceImpl geofenceZoneService;

    private Company company;
    private GeofenceZone circleZone;
    private GeofenceZone rectangleZone;
    private GeofenceZone polygonZone;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        // Circle zone
        circleZone = GeofenceZone.builder()
                .id(1L)
                .name("Downtown Circle")
                .description("Downtown area coverage")
                .company(company)
                .type(GeofenceType.CIRCLE)
                .geometry(Map.of(
                        "center_lat", 40.7128,
                        "center_lng", -74.0060,
                        "radius", 1000.0
                ))
                .isActive(true)
                .color("#FF0000")
                .build();

        // Rectangle zone
        rectangleZone = GeofenceZone.builder()
                .id(2L)
                .name("Shopping District")
                .description("Shopping area")
                .company(company)
                .type(GeofenceType.RECTANGLE)
                .geometry(Map.of(
                        "north", 40.75,
                        "south", 40.70,
                        "east", -73.95,
                        "west", -74.05
                ))
                .isActive(true)
                .color("#00FF00")
                .build();

        // Polygon zone
        polygonZone = GeofenceZone.builder()
                .id(3L)
                .name("Custom Area")
                .description("Custom polygon area")
                .company(company)
                .type(GeofenceType.POLYGON)
                .geometry(Map.of(
                        "coordinates", Arrays.asList(
                                Map.of("lat", 40.71, "lng", -74.01),
                                Map.of("lat", 40.72, "lng", -74.00),
                                Map.of("lat", 40.71, "lng", -73.99)
                        )
                ))
                .isActive(false)
                .color("#0000FF")
                .build();
    }

    @Test
    void createZone_Success() {
        // Arrange
        when(geofenceZoneRepository.save(any(GeofenceZone.class)))
                .thenReturn(circleZone);

        // Act
        GeofenceZone result = geofenceZoneService.createZone(circleZone);

        // Assert
        assertNotNull(result);
        assertEquals(circleZone.getName(), result.getName());
        assertEquals(GeofenceType.CIRCLE, result.getType());
        verify(geofenceZoneRepository).save(circleZone);
    }

    @Test
    void createZone_WithAllFields() {
        // Arrange
        GeofenceZone fullZone = GeofenceZone.builder()
                .name("Full Zone")
                .description("Zone with all fields")
                .company(company)
                .type(GeofenceType.CIRCLE)
                .geometry(Map.of("center_lat", 40.0, "center_lng", -74.0, "radius", 500.0))
                .isActive(true)
                .color("#FFAA00")
                .metadata(Map.of("priority", "high", "category", "commercial"))
                .build();

        when(geofenceZoneRepository.save(any(GeofenceZone.class)))
                .thenReturn(fullZone);

        // Act
        GeofenceZone result = geofenceZoneService.createZone(fullZone);

        // Assert
        assertNotNull(result);
        assertEquals("#FFAA00", result.getColor());
        assertNotNull(result.getMetadata());
        assertEquals("high", result.getMetadata().get("priority"));
        verify(geofenceZoneRepository).save(fullZone);
    }

    @Test
    void updateZone_Success() {
        // Arrange
        GeofenceZone updatedData = GeofenceZone.builder()
                .name("Updated Name")
                .description("Updated description")
                .type(GeofenceType.RECTANGLE)
                .geometry(Map.of("north", 41.0, "south", 40.0, "east", -73.0, "west", -75.0))
                .isActive(false)
                .color("#AA00FF")
                .metadata(Map.of("updated", "true"))
                .build();

        when(geofenceZoneRepository.findById(1L))
                .thenReturn(Optional.of(circleZone));
        when(geofenceZoneRepository.save(any(GeofenceZone.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        GeofenceZone result = geofenceZoneService.updateZone(1L, updatedData);

        // Assert
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertEquals(GeofenceType.RECTANGLE, result.getType());
        assertFalse(result.getIsActive());
        assertEquals("#AA00FF", result.getColor());
        assertNotNull(result.getMetadata());
        verify(geofenceZoneRepository).findById(1L);
        verify(geofenceZoneRepository).save(circleZone);
    }

    @Test
    void updateZone_NotFound() {
        // Arrange
        when(geofenceZoneRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                geofenceZoneService.updateZone(999L, circleZone));
        verify(geofenceZoneRepository).findById(999L);
        verify(geofenceZoneRepository, never()).save(any());
    }

    @Test
    void deleteZone_Success() {
        // Arrange
        doNothing().when(geofenceZoneRepository).deleteById(1L);

        // Act
        geofenceZoneService.deleteZone(1L);

        // Assert
        verify(geofenceZoneRepository).deleteById(1L);
    }

    @Test
    void getZoneById_Found() {
        // Arrange
        when(geofenceZoneRepository.findById(1L))
                .thenReturn(Optional.of(circleZone));

        // Act
        Optional<GeofenceZone> result = geofenceZoneService.getZoneById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(circleZone.getId(), result.get().getId());
        verify(geofenceZoneRepository).findById(1L);
    }

    @Test
    void getZoneById_NotFound() {
        // Arrange
        when(geofenceZoneRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        Optional<GeofenceZone> result = geofenceZoneService.getZoneById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(geofenceZoneRepository).findById(999L);
    }

    @Test
    void getZonesByCompany_Success() {
        // Arrange
        List<GeofenceZone> zones = Arrays.asList(circleZone, rectangleZone, polygonZone);
        when(geofenceZoneRepository.findByCompany_IdOrderByCreatedAtDesc(1L))
                .thenReturn(zones);

        // Act
        List<GeofenceZone> result = geofenceZoneService.getZonesByCompany(1L);

        // Assert
        assertEquals(3, result.size());
        verify(geofenceZoneRepository).findByCompany_IdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getZonesByCompany_EmptyList() {
        // Arrange
        when(geofenceZoneRepository.findByCompany_IdOrderByCreatedAtDesc(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<GeofenceZone> result = geofenceZoneService.getZonesByCompany(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(geofenceZoneRepository).findByCompany_IdOrderByCreatedAtDesc(999L);
    }

    @Test
    void getActiveZonesByCompany_Success() {
        // Arrange
        List<GeofenceZone> activeZones = Arrays.asList(circleZone, rectangleZone);
        when(geofenceZoneRepository.findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(1L))
                .thenReturn(activeZones);

        // Act
        List<GeofenceZone> result = geofenceZoneService.getActiveZonesByCompany(1L);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(GeofenceZone::getIsActive));
        verify(geofenceZoneRepository).findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(1L);
    }

    @Test
    void getActiveZonesByCompany_EmptyList() {
        // Arrange
        when(geofenceZoneRepository.findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<GeofenceZone> result = geofenceZoneService.getActiveZonesByCompany(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(geofenceZoneRepository).findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(999L);
    }

    @Test
    void findZonesContainingPoint_Success() {
        // Arrange
        double testLat = 40.7128;
        double testLng = -74.0060;
        List<GeofenceZone> activeZones = Arrays.asList(circleZone, rectangleZone);
        
        when(geofenceZoneRepository.findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(1L))
                .thenReturn(activeZones);

        // Act
        List<GeofenceZone> result = geofenceZoneService.findZonesContainingPoint(1L, testLat, testLng);

        // Assert
        assertNotNull(result);
        verify(geofenceZoneRepository).findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(1L);
    }

    @Test
    void findZonesContainingPoint_NoActiveZones() {
        // Arrange
        when(geofenceZoneRepository.findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(999L))
                .thenReturn(Arrays.asList());

        // Act
        List<GeofenceZone> result = geofenceZoneService.findZonesContainingPoint(999L, 40.0, -74.0);

        // Assert
        assertTrue(result.isEmpty());
        verify(geofenceZoneRepository).findByCompany_IdAndIsActiveTrueOrderByCreatedAtDesc(999L);
    }

    @Test
    void toggleZoneActive_ActivateInactive() {
        // Arrange
        when(geofenceZoneRepository.findById(3L))
                .thenReturn(Optional.of(polygonZone));
        when(geofenceZoneRepository.save(any(GeofenceZone.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertFalse(polygonZone.getIsActive());

        // Act
        GeofenceZone result = geofenceZoneService.toggleZoneActive(3L, true);

        // Assert
        assertTrue(result.getIsActive());
        verify(geofenceZoneRepository).findById(3L);
        verify(geofenceZoneRepository).save(polygonZone);
    }

    @Test
    void toggleZoneActive_DeactivateActive() {
        // Arrange
        when(geofenceZoneRepository.findById(1L))
                .thenReturn(Optional.of(circleZone));
        when(geofenceZoneRepository.save(any(GeofenceZone.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(circleZone.getIsActive());

        // Act
        GeofenceZone result = geofenceZoneService.toggleZoneActive(1L, false);

        // Assert
        assertFalse(result.getIsActive());
        verify(geofenceZoneRepository).findById(1L);
        verify(geofenceZoneRepository).save(circleZone);
    }

    @Test
    void toggleZoneActive_NotFound() {
        // Arrange
        when(geofenceZoneRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                geofenceZoneService.toggleZoneActive(999L, true));
        verify(geofenceZoneRepository).findById(999L);
        verify(geofenceZoneRepository, never()).save(any());
    }

    @Test
    void isZoneNameUnique_NewZone() {
        // Arrange
        when(geofenceZoneRepository.findByNameAndCompany_Id("New Zone", 1L))
                .thenReturn(Optional.empty());

        // Act
        boolean result = geofenceZoneService.isZoneNameUnique("New Zone", 1L, null);

        // Assert
        assertTrue(result);
        verify(geofenceZoneRepository).findByNameAndCompany_Id("New Zone", 1L);
    }

    @Test
    void isZoneNameUnique_ExistingZone_DifferentId() {
        // Arrange
        when(geofenceZoneRepository.findByNameAndCompany_Id("Downtown Circle", 1L))
                .thenReturn(Optional.of(circleZone));

        // Act
        boolean result = geofenceZoneService.isZoneNameUnique("Downtown Circle", 1L, 999L);

        // Assert
        assertFalse(result);
        verify(geofenceZoneRepository).findByNameAndCompany_Id("Downtown Circle", 1L);
    }

    @Test
    void isZoneNameUnique_ExistingZone_SameId() {
        // Arrange
        when(geofenceZoneRepository.findByNameAndCompany_Id("Downtown Circle", 1L))
                .thenReturn(Optional.of(circleZone));

        // Act
        boolean result = geofenceZoneService.isZoneNameUnique("Downtown Circle", 1L, 1L);

        // Assert
        assertTrue(result);
        verify(geofenceZoneRepository).findByNameAndCompany_Id("Downtown Circle", 1L);
    }

    @Test
    void countActiveZones_Success() {
        // Arrange
        when(geofenceZoneRepository.countByCompany_IdAndIsActiveTrue(1L))
                .thenReturn(2L);

        // Act
        Long result = geofenceZoneService.countActiveZones(1L);

        // Assert
        assertEquals(2L, result);
        verify(geofenceZoneRepository).countByCompany_IdAndIsActiveTrue(1L);
    }

    @Test
    void countActiveZones_Zero() {
        // Arrange
        when(geofenceZoneRepository.countByCompany_IdAndIsActiveTrue(999L))
                .thenReturn(0L);

        // Act
        Long result = geofenceZoneService.countActiveZones(999L);

        // Assert
        assertEquals(0L, result);
        verify(geofenceZoneRepository).countByCompany_IdAndIsActiveTrue(999L);
    }
}
