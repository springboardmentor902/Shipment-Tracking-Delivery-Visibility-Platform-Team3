package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.RouteService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final UserRepository userRepository;

    @Override
    public RouteResponse createRoute(RouteRequest request) {

        if (routeRepository.findByShipmentId(request.getShipmentId()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Route already exists for this shipment"
            );
        }

        Shipment shipment = new Shipment();
        shipment.setId(request.getShipmentId());

        User driver = null;

        if (request.getDriverId() != null) {
            driver = userRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Driver not found with id: " + request.getDriverId()
                    ));
        }

        Route route = Route.builder()
                .shipment(shipment)
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .waypoints(request.getWaypoints())
                .distanceKm(request.getDistanceKm())
                .estimatedTimeMinutes(request.getEstimatedTimeMinutes())
                .actualTimeMinutes(request.getActualTimeMinutes())
                .trafficCondition(request.getTrafficCondition())
                .driver(driver)
                .build();

        Route savedRoute = routeRepository.save(route);

        return mapToResponse(savedRoute);
    }

    @Override
    public RouteResponse assignDriver(Long routeId, Long driverId) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Route not found with id: " + routeId
                ));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Driver not found with id: " + driverId
                ));

        route.setDriver(driver);

        Route updatedRoute = routeRepository.save(route);

        return mapToResponse(updatedRoute);
    }

    @Override
    public RouteResponse getRouteByShipmentId(Long shipmentId) {

        Route route = routeRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Route not found for shipment id: " + shipmentId
                ));

        return mapToResponse(route);
    }

    private RouteResponse mapToResponse(Route route) {

        return RouteResponse.builder()
                .id(route.getId())
                .shipmentId(route.getShipment().getId())
                .origin(route.getOrigin())
                .destination(route.getDestination())
                .waypoints(route.getWaypoints())
                .distanceKm(route.getDistanceKm())
                .estimatedTimeMinutes(route.getEstimatedTimeMinutes())
                .actualTimeMinutes(route.getActualTimeMinutes())
                .trafficCondition(route.getTrafficCondition())
                .driverId(
                        route.getDriver() != null
                                ? route.getDriver().getId()
                                : null
                )
                .build();
    }
}