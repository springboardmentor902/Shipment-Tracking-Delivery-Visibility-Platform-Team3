package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;

public interface RouteService {

    RouteResponse saveRoute(RouteRequest request);

    RouteResponse getRoute(Long shipmentId);
}
