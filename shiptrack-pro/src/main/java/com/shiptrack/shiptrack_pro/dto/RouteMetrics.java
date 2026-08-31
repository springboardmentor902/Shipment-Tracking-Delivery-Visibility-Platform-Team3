package com.shiptrack.shiptrack_pro.dto;

import java.math.BigDecimal;

/**
 * Distance and duration for one route leg.
 *
 * @param source LIVE_MAPS when Google answered, STRAIGHT_LINE when we fell back
 *               to a Haversine estimate because Maps was unavailable.
 */
public record RouteMetrics(BigDecimal distanceKm,
                           Integer durationMinutes,
                           Integer durationInTrafficMinutes,
                           String trafficCondition,
                           String source) {

    public static final String SOURCE_LIVE_MAPS = "LIVE_MAPS";
    public static final String SOURCE_STRAIGHT_LINE = "STRAIGHT_LINE";
}
