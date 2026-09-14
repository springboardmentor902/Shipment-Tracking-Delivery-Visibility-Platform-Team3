package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.RouteAlternativeDTO;
import com.shiptrack.shiptrack_pro.service.RouteOptimizationService;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class RouteOptimizationServiceImpl
        implements RouteOptimizationService {

    // =========================================================
    // SELECT BEST ROUTE
    // =========================================================

    @Override
    public RouteAlternativeDTO selectBestRoute(
            List<RouteAlternativeDTO> routes) {

        if (routes == null || routes.isEmpty()) {
            throw new IllegalArgumentException(
                    "No route alternatives available"
            );
        }

        return routes.stream()
                .filter(route -> route != null)
                .filter(route ->
                        route.getDurationMinutes() != null
                )
                .min(
                        Comparator.comparingInt(
                                RouteAlternativeDTO::getDurationMinutes
                        )
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No valid route with duration available"
                        )
                );
    }

    // =========================================================
    // GET SELECTION REASON
    // =========================================================

    @Override
    public String getSelectionReason(
            List<RouteAlternativeDTO> routes,
            RouteAlternativeDTO selectedRoute) {

        if (selectedRoute == null) {
            return "No route was selected.";
        }

        if (routes == null || routes.isEmpty()) {
            return "The selected route was the only available route.";
        }

        int selectedDuration =
                selectedRoute.getDurationMinutes();

        boolean hasFasterAlternative =
                routes.stream()
                        .filter(route -> route != null)
                        .filter(route ->
                                route.getDurationMinutes() != null
                        )
                        .anyMatch(route ->
                                route.getDurationMinutes()
                                        < selectedDuration
                        );

        if (hasFasterAlternative) {
            return "A faster route is available.";
        }

        return "Selected because it has the lowest estimated "
                + "travel duration among the available OSRM routes.";
    }
}