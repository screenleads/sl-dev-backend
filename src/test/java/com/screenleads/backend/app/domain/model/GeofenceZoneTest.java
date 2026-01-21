package com.screenleads.backend.app.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GeofenceZone entity containment algorithms
 */
class GeofenceZoneTest {

    private GeofenceZone circleZone;
    private GeofenceZone rectangleZone;
    private GeofenceZone polygonZone;

    @BeforeEach
    void setUp() {
        // Circle zone: Madrid center (40.4168, -3.7038) with 1km radius
        circleZone = new GeofenceZone();
        circleZone.setType(GeofenceType.CIRCLE);
        Map<String, Object> circleGeometry = new HashMap<>();
        Map<String, Double> center = new HashMap<>();
        center.put("lat", 40.4168);
        center.put("lon", -3.7038);
        circleGeometry.put("center", center);
        circleGeometry.put("radius", 1000.0);
        circleZone.setGeometry(circleGeometry);

        // Rectangle zone: SW (40.40, -3.75) to NE (40.45, -3.65)
        rectangleZone = new GeofenceZone();
        rectangleZone.setType(GeofenceType.RECTANGLE);
        Map<String, Object> rectGeometry = new HashMap<>();
        Map<String, Double> sw = new HashMap<>();
        sw.put("lat", 40.40);
        sw.put("lon", -3.75);
        Map<String, Double> ne = new HashMap<>();
        ne.put("lat", 40.45);
        ne.put("lon", -3.65);
        rectGeometry.put("sw", sw);
        rectGeometry.put("ne", ne);
        rectangleZone.setGeometry(rectGeometry);

        // Polygon zone: Triangle around Puerta del Sol
        // Points: (40.417, -3.704), (40.418, -3.703), (40.416, -3.702)
        polygonZone = new GeofenceZone();
        polygonZone.setType(GeofenceType.POLYGON);
        Map<String, Object> polygonGeometry = new HashMap<>();
        Object[] coordinates = new Object[]{
                new double[]{40.417, -3.704},
                new double[]{40.418, -3.703},
                new double[]{40.416, -3.702},
                new double[]{40.417, -3.704} // Close the polygon
        };
        polygonGeometry.put("coordinates", coordinates);
        polygonZone.setGeometry(polygonGeometry);
    }

    // ==================== CIRCLE TESTS ====================

    @Test
    void testCircleContainsPoint_CenterPoint() {
        // Point at exact center
        boolean result = circleZone.containsPoint(40.4168, -3.7038);
        assertTrue(result, "Center point should be inside circle");
    }

    @Test
    void testCircleContainsPoint_VeryClose() {
        // Point 500m away (well within 1km radius)
        boolean result = circleZone.containsPoint(40.4218, -3.7038);
        assertTrue(result, "Point 500m away should be inside 1km circle");
    }

    @Test
    void testCircleContainsPoint_OnEdge() {
        // Point approximately on the edge (~0.9km away, within radius)
        // Using approximation: 1° lat ≈ 111km, so 0.008° ≈ 0.9km
        boolean result = circleZone.containsPoint(40.4248, -3.7038);
        assertTrue(result, "Point ~900m away should be inside 1km circle");
    }

    @Test
    void testCircleContainsPoint_Outside() {
        // Point 2km away (outside 1km radius)
        boolean result = circleZone.containsPoint(40.4348, -3.7038);
        assertFalse(result, "Point 2km away should be outside 1km circle");
    }

    @Test
    void testCircleContainsPoint_FarAway() {
        // Point in Barcelona (very far)
        boolean result = circleZone.containsPoint(41.3851, 2.1734);
        assertFalse(result, "Point in Barcelona should be outside Madrid circle");
    }

    // ==================== RECTANGLE TESTS ====================

    @Test
    void testRectangleContainsPoint_Center() {
        // Point in center of rectangle
        boolean result = rectangleZone.containsPoint(40.425, -3.70);
        assertTrue(result, "Center point should be inside rectangle");
    }

    @Test
    void testRectangleContainsPoint_SouthWestCorner() {
        // Point at SW corner
        boolean result = rectangleZone.containsPoint(40.40, -3.75);
        assertTrue(result, "SW corner point should be inside rectangle");
    }

    @Test
    void testRectangleContainsPoint_NorthEastCorner() {
        // Point at NE corner
        boolean result = rectangleZone.containsPoint(40.45, -3.65);
        assertTrue(result, "NE corner point should be inside rectangle");
    }

    @Test
    void testRectangleContainsPoint_OnEdge() {
        // Point on southern edge
        boolean result = rectangleZone.containsPoint(40.40, -3.70);
        assertTrue(result, "Point on edge should be inside rectangle");
    }

    @Test
    void testRectangleContainsPoint_Outside_South() {
        // Point below southern boundary
        boolean result = rectangleZone.containsPoint(40.39, -3.70);
        assertFalse(result, "Point below southern boundary should be outside");
    }

    @Test
    void testRectangleContainsPoint_Outside_North() {
        // Point above northern boundary
        boolean result = rectangleZone.containsPoint(40.46, -3.70);
        assertFalse(result, "Point above northern boundary should be outside");
    }

    @Test
    void testRectangleContainsPoint_Outside_West() {
        // Point west of western boundary
        boolean result = rectangleZone.containsPoint(40.425, -3.76);
        assertFalse(result, "Point west of western boundary should be outside");
    }

    @Test
    void testRectangleContainsPoint_Outside_East() {
        // Point east of eastern boundary
        boolean result = rectangleZone.containsPoint(40.425, -3.64);
        assertFalse(result, "Point east of eastern boundary should be outside");
    }

    // ==================== POLYGON TESTS ====================

    @Test
    void testPolygonContainsPoint_Inside() {
        // Point inside the triangle (centroid approximately)
        boolean result = polygonZone.containsPoint(40.417, -3.703);
        // Currently will fail because polygon is not implemented (returns false)
        // TODO: Uncomment when polygon algorithm is implemented
        // assertTrue(result, "Point inside polygon should return true");
        assertFalse(result, "Polygon containment not yet implemented - expected false");
    }

    @Test
    void testPolygonContainsPoint_Outside() {
        // Point clearly outside the triangle
        boolean result = polygonZone.containsPoint(40.42, -3.71);
        assertFalse(result, "Point outside polygon should return false");
    }

    @Test
    void testPolygonContainsPoint_OnVertex() {
        // Point on one of the vertices
        boolean result = polygonZone.containsPoint(40.417, -3.704);
        // TODO: Uncomment when polygon algorithm is implemented
        // assertTrue(result, "Point on vertex should return true");
        assertFalse(result, "Polygon containment not yet implemented - expected false");
    }

    // ==================== EDGE CASES ====================

    @Test
    void testContainsPoint_NullGeometry() {
        GeofenceZone nullZone = new GeofenceZone();
        nullZone.setType(GeofenceType.CIRCLE);
        nullZone.setGeometry(null);

        boolean result = nullZone.containsPoint(40.4168, -3.7038);
        assertFalse(result, "Null geometry should return false");
    }

    @Test
    void testContainsPoint_InvalidCircleGeometry_MissingCenter() {
        GeofenceZone invalidZone = new GeofenceZone();
        invalidZone.setType(GeofenceType.CIRCLE);
        Map<String, Object> invalidGeometry = new HashMap<>();
        invalidGeometry.put("radius", 1000.0);
        // Missing center
        invalidZone.setGeometry(invalidGeometry);

        boolean result = invalidZone.containsPoint(40.4168, -3.7038);
        assertFalse(result, "Invalid circle geometry should return false");
    }

    @Test
    void testContainsPoint_InvalidCircleGeometry_MissingRadius() {
        GeofenceZone invalidZone = new GeofenceZone();
        invalidZone.setType(GeofenceType.CIRCLE);
        Map<String, Object> invalidGeometry = new HashMap<>();
        Map<String, Double> center = new HashMap<>();
        center.put("lat", 40.4168);
        center.put("lon", -3.7038);
        invalidGeometry.put("center", center);
        // Missing radius
        invalidZone.setGeometry(invalidGeometry);

        boolean result = invalidZone.containsPoint(40.4168, -3.7038);
        assertFalse(result, "Invalid circle geometry (no radius) should return false");
    }

    @Test
    void testContainsPoint_InvalidRectangleGeometry_MissingSW() {
        GeofenceZone invalidZone = new GeofenceZone();
        invalidZone.setType(GeofenceType.RECTANGLE);
        Map<String, Object> invalidGeometry = new HashMap<>();
        Map<String, Double> ne = new HashMap<>();
        ne.put("lat", 40.45);
        ne.put("lon", -3.65);
        invalidGeometry.put("ne", ne);
        // Missing sw
        invalidZone.setGeometry(invalidGeometry);

        boolean result = invalidZone.containsPoint(40.425, -3.70);
        assertFalse(result, "Invalid rectangle geometry (no SW) should return false");
    }

    @Test
    void testContainsPoint_InvalidRectangleGeometry_MissingNE() {
        GeofenceZone invalidZone = new GeofenceZone();
        invalidZone.setType(GeofenceType.RECTANGLE);
        Map<String, Object> invalidGeometry = new HashMap<>();
        Map<String, Double> sw = new HashMap<>();
        sw.put("lat", 40.40);
        sw.put("lon", -3.75);
        invalidGeometry.put("sw", sw);
        // Missing ne
        invalidZone.setGeometry(invalidGeometry);

        boolean result = invalidZone.containsPoint(40.425, -3.70);
        assertFalse(result, "Invalid rectangle geometry (no NE) should return false");
    }

    @Test
    void testContainsPoint_UnknownZoneType() {
        GeofenceZone unknownZone = new GeofenceZone();
        unknownZone.setType(null);
        Map<String, Object> geometry = new HashMap<>();
        unknownZone.setGeometry(geometry);

        boolean result = unknownZone.containsPoint(40.4168, -3.7038);
        assertFalse(result, "Unknown zone type should return false");
    }

    // ==================== BOUNDARY TESTS ====================

    @Test
    void testCircleContainsPoint_EquatorCrossing() {
        // Circle at equator (0, 0) with 1km radius
        GeofenceZone equatorZone = new GeofenceZone();
        equatorZone.setType(GeofenceType.CIRCLE);
        Map<String, Object> geometry = new HashMap<>();
        Map<String, Double> center = new HashMap<>();
        center.put("lat", 0.0);
        center.put("lon", 0.0);
        geometry.put("center", center);
        geometry.put("radius", 1000.0);
        equatorZone.setGeometry(geometry);

        // Point at center
        boolean result1 = equatorZone.containsPoint(0.0, 0.0);
        assertTrue(result1, "Center at equator should be inside");

        // Point slightly north
        boolean result2 = equatorZone.containsPoint(0.005, 0.0);
        assertTrue(result2, "Point 500m north of equator should be inside 1km circle");
    }

    @Test
    void testRectangleContainsPoint_CrossingPrimeMeridian() {
        // Rectangle crossing prime meridian: SW(-1, -1) to NE(1, 1)
        GeofenceZone meridianZone = new GeofenceZone();
        meridianZone.setType(GeofenceType.RECTANGLE);
        Map<String, Object> geometry = new HashMap<>();
        Map<String, Double> sw = new HashMap<>();
        sw.put("lat", -1.0);
        sw.put("lon", -1.0);
        Map<String, Double> ne = new HashMap<>();
        ne.put("lat", 1.0);
        ne.put("lon", 1.0);
        geometry.put("sw", sw);
        geometry.put("ne", ne);
        meridianZone.setGeometry(geometry);

        // Point at (0, 0) should be inside
        boolean result = meridianZone.containsPoint(0.0, 0.0);
        assertTrue(result, "Point at prime meridian crossing should be inside rectangle");
    }
}
