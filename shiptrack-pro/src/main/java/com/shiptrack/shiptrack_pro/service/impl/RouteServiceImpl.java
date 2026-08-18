package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.entity.DeliveryRoute;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.repository.DeliveryRouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.RouteService;
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final DeliveryRouteRepository deliveryRouteRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentService shipmentService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public RouteResponse saveRoute(RouteRequest request) {
        Role role = currentUserService.getCurrentRole();
        if (role != Role.LOGISTICS_OPERATOR && role != Role.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only LOGISTICS_OPERATOR and ADMINISTRATOR can save routes");
        }

        Shipment shipment = findShipment(request.getShipmentId());
        DeliveryRoute route = deliveryRouteRepository.findByShipmentId(shipment.getId())
                .orElseGet(() -> DeliveryRoute.builder().shipment(shipment).build());
        route.setOriginAddress(request.getOriginAddress());
        route.setDestinationAddress(request.getDestinationAddress());
        route.setWaypoints(request.getWaypoints());
        route.setDistanceKm(request.getDistanceKm());
        route.setExpectedDurationMinutes(request.getExpectedDurationMinutes());

        return toResponse(deliveryRouteRepository.save(route));
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getRoute(Long shipmentId) {
        shipmentService.getShipmentById(shipmentId);
        return deliveryRouteRepository.findByShipmentId(shipmentId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Route not found for shipment id: " + shipmentId));
    }

    private Shipment findShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + shipmentId));
    }

    private RouteResponse toResponse(DeliveryRoute route) {
        return RouteResponse.builder()
                .id(route.getId())
                .shipmentId(route.getShipment().getId())
                .originAddress(route.getOriginAddress())
                .destinationAddress(route.getDestinationAddress())
                .waypoints(route.getWaypoints())
                .distanceKm(route.getDistanceKm())
                .expectedDurationMinutes(route.getExpectedDurationMinutes())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}
