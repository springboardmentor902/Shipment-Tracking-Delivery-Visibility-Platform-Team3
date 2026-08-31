package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.dto.RouteUpdateRequest;
import com.shiptrack.shiptrack_pro.entity.DeliveryRoute;
import com.shiptrack.shiptrack_pro.entity.RouteLegStatus;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.DeliveryRouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.RouteService;
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final DeliveryRouteRepository deliveryRouteRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final ShipmentService shipmentService;
    private final CurrentUserService currentUserService;

    /* ===================== CREATE ===================== */

    @Override
    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        User actor = currentUserService.getCurrentUser();
        Role role = Role.valueOf(actor.getRole());
        assertCanManage(role);

        Shipment shipment = findShipment(request.getShipmentId());

        // An operator may only plan work on shipments they are responsible for.
        if (role == Role.LOGISTICS_OPERATOR && !isOperatorOfShipment(shipment, actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not the assigned operator for this shipment");
        }

        Integer legNumber = request.getLegNumber();
        if (legNumber == null) {
            legNumber = nextLegNumber(shipment.getId());
        } else if (deliveryRouteRepository.existsByShipmentIdAndLegNumber(shipment.getId(), legNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Leg number " + legNumber + " already exists for shipment " + shipment.getId());
        }

        DeliveryRoute route = DeliveryRoute.builder()
                .shipment(shipment)
                .legNumber(legNumber)
                .driver(request.getDriverId() == null ? null : resolveDriver(request.getDriverId()))
                .originAddress(request.getOriginAddress())
                .destinationAddress(request.getDestinationAddress())
                .waypoints(request.getWaypoints())
                .originLatitude(request.getOriginLatitude())
                .originLongitude(request.getOriginLongitude())
                .destinationLatitude(request.getDestinationLatitude())
                .destinationLongitude(request.getDestinationLongitude())
                .distanceKm(request.getDistanceKm())
                .expectedDurationMinutes(request.getExpectedDurationMinutes())
                .durationInTrafficMinutes(request.getDurationInTrafficMinutes())
                .trafficCondition(request.getTrafficCondition())
                .notes(request.getNotes())
                .status(RouteLegStatus.PLANNED)
                .build();

        return toResponse(deliveryRouteRepository.save(route));
    }

    /* ===================== UPDATE ===================== */

    @Override
    @Transactional
    public RouteResponse updateRoute(Long routeId, RouteUpdateRequest request) {
        User actor = currentUserService.getCurrentUser();
        Role role = Role.valueOf(actor.getRole());
        assertCanManage(role);

        DeliveryRoute route = findRoute(routeId);

        // Operators may only touch legs on their own shipments or legs they drive.
        if (role == Role.LOGISTICS_OPERATOR
                && !isOperatorOfShipment(route.getShipment(), actor)
                && !isDriverOfLeg(route, actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only manage route legs of shipments assigned to you");
        }

        if (request.getLegNumber() != null
                && !request.getLegNumber().equals(route.getLegNumber())) {
            Long shipmentId = route.getShipment().getId();
            if (deliveryRouteRepository.existsByShipmentIdAndLegNumber(shipmentId, request.getLegNumber())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Leg number " + request.getLegNumber() + " already exists for shipment " + shipmentId);
            }
            route.setLegNumber(request.getLegNumber());
        }

        if (request.getDriverId() != null) {
            route.setDriver(resolveDriver(request.getDriverId()));
        }

        if (request.getOriginAddress() != null) {
            route.setOriginAddress(request.getOriginAddress());
            // address changed, previously geocoded coordinates are no longer trustworthy
            route.setOriginLatitude(null);
            route.setOriginLongitude(null);
        }
        if (request.getDestinationAddress() != null) {
            route.setDestinationAddress(request.getDestinationAddress());
            route.setDestinationLatitude(null);
            route.setDestinationLongitude(null);
        }

        if (request.getWaypoints() != null)              route.setWaypoints(request.getWaypoints());
        if (request.getOriginLatitude() != null)         route.setOriginLatitude(request.getOriginLatitude());
        if (request.getOriginLongitude() != null)        route.setOriginLongitude(request.getOriginLongitude());
        if (request.getDestinationLatitude() != null)    route.setDestinationLatitude(request.getDestinationLatitude());
        if (request.getDestinationLongitude() != null)   route.setDestinationLongitude(request.getDestinationLongitude());
        if (request.getDistanceKm() != null)             route.setDistanceKm(request.getDistanceKm());
        if (request.getExpectedDurationMinutes() != null) route.setExpectedDurationMinutes(request.getExpectedDurationMinutes());
        if (request.getDurationInTrafficMinutes() != null) route.setDurationInTrafficMinutes(request.getDurationInTrafficMinutes());
        if (request.getTrafficCondition() != null)       route.setTrafficCondition(request.getTrafficCondition());
        if (request.getNotes() != null)                  route.setNotes(request.getNotes());
        if (request.getStatus() != null)                 route.setStatus(parseStatus(request.getStatus()));

        return toResponse(deliveryRouteRepository.save(route));
    }

    /* ===================== READ ===================== */

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponse> getRoutes(Long shipmentId) {
        // throws 403/404 when the caller may not see this shipment
        shipmentService.getShipmentById(shipmentId);

        return deliveryRouteRepository.findByShipmentIdOrderByLegNumberAsc(shipmentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getRoute(Long routeId) {
        DeliveryRoute route = findRoute(routeId);
        shipmentService.getShipmentById(route.getShipment().getId());
        return toResponse(route);
    }

    /* ===================== helpers ===================== */

    private void assertCanManage(Role role) {
        if (role != Role.LOGISTICS_OPERATOR && role != Role.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only LOGISTICS_OPERATOR and ADMINISTRATOR can manage routes. Your role: " + role);
        }
    }

    private boolean isOperatorOfShipment(Shipment shipment, User actor) {
        return shipment.getAssignedOperator() != null
                && Objects.equals(shipment.getAssignedOperator().getId(), actor.getId());
    }

    private boolean isDriverOfLeg(DeliveryRoute route, User actor) {
        return route.getDriver() != null
                && Objects.equals(route.getDriver().getId(), actor.getId());
    }

    private Integer nextLegNumber(Long shipmentId) {
        Integer max = deliveryRouteRepository.findMaxLegNumber(shipmentId);
        return (max == null ? 0 : max) + 1;
    }

    private Shipment findShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + shipmentId));
    }

    private DeliveryRoute findRoute(Long routeId) {
        return deliveryRouteRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Route not found with id: " + routeId));
    }

    private User resolveDriver(Long driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + driverId));

        if (!Role.LOGISTICS_OPERATOR.name().equals(driver.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User " + driverId + " is not a LOGISTICS_OPERATOR and cannot drive a route");
        }
        return driver;
    }

    private RouteLegStatus parseStatus(String raw) {
        try {
            return RouteLegStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid route status: " + raw + ". Must be one of: "
                            + Arrays.toString(RouteLegStatus.values()));
        }
    }

    private RouteResponse toResponse(DeliveryRoute route) {
        Shipment shipment = route.getShipment();
        User driver = route.getDriver();

        return RouteResponse.builder()
                .id(route.getId())
                .shipmentId(shipment.getId())
                .shipmentTrackingNumber(shipment.getTrackingNumber())
                .legNumber(route.getLegNumber())
                .driverId(driver == null ? null : driver.getId())
                .driverName(driver == null ? null : driver.getFullName())
                .originAddress(route.getOriginAddress())
                .destinationAddress(route.getDestinationAddress())
                .waypoints(route.getWaypoints())
                .originLatitude(route.getOriginLatitude())
                .originLongitude(route.getOriginLongitude())
                .destinationLatitude(route.getDestinationLatitude())
                .destinationLongitude(route.getDestinationLongitude())
                .distanceKm(route.getDistanceKm())
                .expectedDurationMinutes(route.getExpectedDurationMinutes())
                .durationInTrafficMinutes(route.getDurationInTrafficMinutes())
                .trafficCondition(route.getTrafficCondition())
                .lastKnownLatitude(route.getLastKnownLatitude())
                .lastKnownLongitude(route.getLastKnownLongitude())
                .lastLocationAt(route.getLastLocationAt())
                .status(route.getStatus() == null ? null : route.getStatus().name())
                .notes(route.getNotes())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}
