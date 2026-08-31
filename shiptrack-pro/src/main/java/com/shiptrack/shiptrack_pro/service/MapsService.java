package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.GeoPoint;
import com.shiptrack.shiptrack_pro.dto.RouteMetrics;

import java.util.Optional;

/**
 * Maps provider abstraction.
 *
 * Every method returns an Optional and never throws: when the API key is
 * missing, the provider is down or the response is unusable, the caller keeps
 * working with whatever data it already has.
 */
public interface MapsService {

    /** True when a Maps API key is configured. */
    boolean isEnabled();

    /** Address text to coordinates. */
    Optional<GeoPoint> geocode(String address);

    /**
     * Distance, free-flow duration and traffic-aware duration between two points.
     *
     * @param waypoints optional semicolon-separated stops, may be null or blank
     */
    Optional<RouteMetrics> routeMetrics(GeoPoint origin, GeoPoint destination, String waypoints);
}
