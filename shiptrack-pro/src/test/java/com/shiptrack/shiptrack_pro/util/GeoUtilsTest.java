package com.shiptrack.shiptrack_pro.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoUtilsTest {

    private static final BigDecimal HYDERABAD_LAT = new BigDecimal("17.385044");
    private static final BigDecimal HYDERABAD_LNG = new BigDecimal("78.486671");
    private static final BigDecimal BENGALURU_LAT = new BigDecimal("12.971599");
    private static final BigDecimal BENGALURU_LNG = new BigDecimal("77.594566");

    @Test
    @DisplayName("straight-line distance Hyderabad to Bengaluru is about 500 km")
    void haversineIsRoughlyCorrect() {
        BigDecimal km = GeoUtils.haversineKm(HYDERABAD_LAT, HYDERABAD_LNG, BENGALURU_LAT, BENGALURU_LNG);
        assertTrue(km.doubleValue() > 480 && km.doubleValue() < 520,
                "expected about 500 km but got " + km);
    }

    @Test
    @DisplayName("estimated road distance pads the straight line")
    void roadEstimateIsLongerThanStraightLine() {
        BigDecimal straight = GeoUtils.haversineKm(HYDERABAD_LAT, HYDERABAD_LNG, BENGALURU_LAT, BENGALURU_LNG);
        BigDecimal road = GeoUtils.estimatedRoadDistanceKm(HYDERABAD_LAT, HYDERABAD_LNG, BENGALURU_LAT, BENGALURU_LNG);
        assertTrue(road.compareTo(straight) > 0, "road estimate should exceed straight line");
    }

    @Test
    @DisplayName("duration estimate is never zero")
    void durationEstimateHasAFloor() {
        assertEquals(1, GeoUtils.estimatedDurationMinutes(new BigDecimal("0.10")));
        assertNull(GeoUtils.estimatedDurationMinutes(null));
    }

    @Test
    @DisplayName("traffic is graded from the delay ratio")
    void trafficClassification() {
        assertEquals("LIGHT", GeoUtils.classifyTraffic(60, 62));
        assertEquals("MODERATE", GeoUtils.classifyTraffic(60, 75));
        assertEquals("HEAVY", GeoUtils.classifyTraffic(60, 95));
        assertEquals("SEVERE", GeoUtils.classifyTraffic(60, 130));
        assertNull(GeoUtils.classifyTraffic(60, null));
        assertNull(GeoUtils.classifyTraffic(null, 90));
    }
}
