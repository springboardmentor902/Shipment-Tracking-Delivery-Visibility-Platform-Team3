package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;

public interface RouteService {

    RouteResponse createRoute(RouteRequest request);

    RouteResponse assignDriver(Long routeId, Long driverId);

    RouteResponse getRouteByShipmentId(Long shipmentId);
}