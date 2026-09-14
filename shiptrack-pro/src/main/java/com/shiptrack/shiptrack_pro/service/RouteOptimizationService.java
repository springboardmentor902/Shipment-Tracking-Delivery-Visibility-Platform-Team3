package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.RouteAlternativeDTO;

import java.util.List;

public interface RouteOptimizationService {

    RouteAlternativeDTO selectBestRoute(List<RouteAlternativeDTO> routes);

    String getSelectionReason(
            List<RouteAlternativeDTO> routes,
            RouteAlternativeDTO selectedRoute
    );
}