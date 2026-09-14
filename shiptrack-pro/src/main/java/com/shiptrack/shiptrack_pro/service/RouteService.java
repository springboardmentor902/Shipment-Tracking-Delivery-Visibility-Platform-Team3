package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.LocationUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;

import java.util.List;

public interface RouteService {

    RouteResponse createRoute(
            RouteRequest request
    );

    RouteResponse assignDriver(
            Long routeId,
            Long driverId
    );

    RouteResponse updateRouteStatus(
            Long routeId,
            String status
    );

    RouteResponse updateRouteLocation(
            Long routeId,
            LocationUpdateRequest request
    );

    RouteResponse refreshRouteFromMaps(
            Long routeId
    );

    RouteResponse getRouteByShipmentId(
            Long shipmentId
    );

    List<RouteResponse> getRouteHistory(
            Long shipmentId
    );
}