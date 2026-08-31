package com.shiptrack.shiptrack_pro.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Small geo helpers that do not need any external service. */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    /** Rough road-distance multiplier applied to straight-line distance. */
    private static final double ROAD_FACTOR = 1.25;

    /** Assumed average road speed in km/h when no Maps duration is available. */
    private static final double AVERAGE_SPEED_KMPH = 40.0;

    private GeoUtils() {
    }

    /** Great-circle distance in kilometres, rounded to two decimals. */
    public static BigDecimal haversineKm(BigDecimal fromLat, BigDecimal fromLng,
                                         BigDecimal toLat, BigDecimal toLng) {
        double lat1 = Math.toRadians(fromLat.doubleValue());
        double lat2 = Math.toRadians(toLat.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(toLng.doubleValue() - fromLng.doubleValue());

        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLng / 2), 2);
        double km = 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));

        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
    }

    /** Straight-line distance padded a little to approximate real roads. */
    public static BigDecimal estimatedRoadDistanceKm(BigDecimal fromLat, BigDecimal fromLng,
                                                     BigDecimal toLat, BigDecimal toLng) {
        return haversineKm(fromLat, fromLng, toLat, toLng)
                .multiply(BigDecimal.valueOf(ROAD_FACTOR))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Very rough travel time for the estimated distance, minimum one minute. */
    public static Integer estimatedDurationMinutes(BigDecimal distanceKm) {
        if (distanceKm == null) {
            return null;
        }
        double minutes = distanceKm.doubleValue() / AVERAGE_SPEED_KMPH * 60.0;
        return (int) Math.max(1, Math.round(minutes));
    }

    /**
     * Labels how bad traffic is by comparing the traffic-aware duration with the
     * free-flow duration.
     */
    public static String classifyTraffic(Integer durationMinutes, Integer durationInTrafficMinutes) {
        if (durationMinutes == null || durationInTrafficMinutes == null || durationMinutes <= 0) {
            return null;
        }
        double ratio = (double) durationInTrafficMinutes / durationMinutes;
        if (ratio < 1.15) return "LIGHT";
        if (ratio < 1.40) return "MODERATE";
        if (ratio < 1.80) return "HEAVY";
        return "SEVERE";
    }
}
