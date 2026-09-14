package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.service.OpenStreetMapService;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.OSRMService;
import com.shiptrack.shiptrack_pro.service.RouteService;
import com.shiptrack.shiptrack_pro.dto.LocationUpdateRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final OSRMService osrmService;
    private final OpenStreetMapService openStreetMapService;

    // =========================================================
    // CREATE ROUTE
    // =========================================================

    @Override
    public RouteResponse createRoute(RouteRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Route request is required"
            );
        }

        if (request.getShipmentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Shipment ID is required"
            );
        }

        if (request.getOrigin() == null
                || request.getOrigin().isBlank()
                || request.getDestination() == null
                || request.getDestination().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Origin and destination are required"
            );
        }

        // -----------------------------------------------------
        // Make previous current route non-current
        // -----------------------------------------------------

        routeRepository
                .findByShipmentIdAndIsCurrentTrue(
                        request.getShipmentId()
                )
                .ifPresent(currentRoute -> {
                    currentRoute.setIsCurrent(false);
                    routeRepository.save(currentRoute);
                });

        // -----------------------------------------------------
        // Shipment reference
        // -----------------------------------------------------

        Shipment shipment = new Shipment();
        shipment.setId(request.getShipmentId());

        // -----------------------------------------------------
        // Driver
        // -----------------------------------------------------

        User driver = null;

        if (request.getDriverId() != null) {

            driver = userRepository
                    .findById(request.getDriverId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Driver not found with id: "
                                            + request.getDriverId()
                            )
                    );
        }

        // -----------------------------------------------------
        // Get route metrics automatically from OpenStreetMap
        // -----------------------------------------------------

        RouteMetrics metrics =
                calculateRouteMetrics(
                        request.getOrigin(),
                        request.getDestination()
                );

        // -----------------------------------------------------
        // Create route
        // -----------------------------------------------------

        Route route = Route.builder()
                .shipment(shipment)
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .waypoints(request.getWaypoints())

                // Automatic OSM/OSRM values
                .distanceKm(metrics.distanceKm())
                .estimatedTimeMinutes(metrics.estimatedTimeMinutes())

                .actualTimeMinutes(
                        request.getActualTimeMinutes()
                )

                .trafficCondition(
                        request.getTrafficCondition() != null
                                ? request.getTrafficCondition()
                                : "NORMAL"
                )

                .driver(driver)
                .isCurrent(true)
                .status("PLANNED")
                .build();

        Route savedRoute =
                routeRepository.save(route);

        return mapToResponse(savedRoute);
    }

    // =========================================================
    // ASSIGN DRIVER
    // =========================================================

    @Override
    public RouteResponse assignDriver(
            Long routeId,
            Long driverId) {

        Route route =
                routeRepository.findById(routeId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Route not found with id: "
                                                + routeId
                                )
                        );

        User driver =
                userRepository.findById(driverId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Driver not found with id: "
                                                + driverId
                                )
                        );

        route.setDriver(driver);

        Route updatedRoute =
                routeRepository.save(route);

        return mapToResponse(updatedRoute);
    }

    // =========================================================
    // UPDATE ROUTE STATUS
    // =========================================================

    @Override
    public RouteResponse updateRouteStatus(
            Long routeId,
            String status) {

        Route route =
                routeRepository.findById(routeId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Route not found with id: "
                                                + routeId
                                )
                        );

        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Route status is required"
            );
        }

        String normalizedStatus =
                status.trim().toUpperCase();

        List<String> allowedStatuses =
                List.of(
                        "PLANNED",
                        "ACTIVE",
                        "COMPLETED",
                        "SKIPPED"
                );

        if (!allowedStatuses.contains(normalizedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid route status: " + status
            );
        }

        route.setStatus(normalizedStatus);

        Route updatedRoute =
                routeRepository.save(route);

        return mapToResponse(updatedRoute);
    }

 // =========================================================
 // UPDATE LIVE ROUTE LOCATION
 // =========================================================

 @Override
 public RouteResponse updateRouteLocation(
         Long routeId,
         LocationUpdateRequest request) {

     if (request == null) {
         throw new ResponseStatusException(
                 HttpStatus.BAD_REQUEST,
                 "Location update request is required"
         );
     }

     if (request.getLatitude() == null) {
         throw new ResponseStatusException(
                 HttpStatus.BAD_REQUEST,
                 "Latitude is required"
         );
     }

     if (request.getLongitude() == null) {
         throw new ResponseStatusException(
                 HttpStatus.BAD_REQUEST,
                 "Longitude is required"
         );
     }

     Route route = routeRepository.findById(routeId)
             .orElseThrow(() ->
                     new ResponseStatusException(
                             HttpStatus.NOT_FOUND,
                             "Route not found with id: " + routeId
                     )
             );

     /*
      * These fields must exist in your Route entity.
      * If your entity uses different names, replace them
      * with the actual field setter names.
      */
     route.setCurrentLatitude(request.getLatitude());
     route.setCurrentLongitude(request.getLongitude());

     if (request.getLocationName() != null
             && !request.getLocationName().isBlank()) {

         route.setCurrentLocation(
                 request.getLocationName()
         );
     }

     if (request.getNotes() != null) {
         route.setLocationNotes(
                 request.getNotes()
         );
     }

     Route updatedRoute = routeRepository.save(route);

     return mapToResponse(updatedRoute);
 }
    // =========================================================
    // REFRESH ROUTE FROM OPENSTREETMAP / OSRM
    // =========================================================

    @Override
    public RouteResponse refreshRouteFromMaps(
            Long routeId) {

        Route route =
                routeRepository.findById(routeId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Route not found with id: "
                                                + routeId
                                )
                        );

        if (route.getOrigin() == null
                || route.getOrigin().isBlank()
                || route.getDestination() == null
                || route.getDestination().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Origin and destination are required."
            );
        }

        // -----------------------------------------------------
        // Calculate fresh OSM/OSRM metrics
        // -----------------------------------------------------

        RouteMetrics metrics =
                calculateRouteMetrics(
                        route.getOrigin(),
                        route.getDestination()
                );

        route.setDistanceKm(
                metrics.distanceKm()
        );

        route.setEstimatedTimeMinutes(
                metrics.estimatedTimeMinutes()
        );

        // Keep current project traffic value
        if (route.getTrafficCondition() == null
                || route.getTrafficCondition().isBlank()) {

            route.setTrafficCondition("NORMAL");
        }

        Route updatedRoute =
                routeRepository.save(route);

        return mapToResponse(updatedRoute);
    }

    // =========================================================
    // GET CURRENT ROUTE
    // =========================================================

    @Override
    public RouteResponse getRouteByShipmentId(
            Long shipmentId) {

        Route route =
                routeRepository
                        .findByShipmentIdAndIsCurrentTrue(
                                shipmentId
                        )
                        .orElse(null);

        if (route == null) {

            List<Route> routes =
                    routeRepository
                            .findAllByShipmentIdOrderByCreatedAtAsc(
                                    shipmentId
                            );

            if (routes.isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No route found for shipment id: "
                                + shipmentId
                );
            }

            route =
                    routes.get(routes.size() - 1);

            route.setIsCurrent(true);

            routeRepository.save(route);
        }

        return mapToResponse(route);
    }

    // =========================================================
    // GET ROUTE HISTORY
    // =========================================================

    @Override
    public List<RouteResponse> getRouteHistory(
            Long shipmentId) {

        return routeRepository
                .findAllByShipmentIdOrderByCreatedAtAsc(
                        shipmentId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // OPENSTREETMAP + OSRM ROUTE CALCULATION
    // =========================================================

    @SuppressWarnings("unchecked")
    private RouteMetrics calculateRouteMetrics(
            String originAddress,
            String destinationAddress) {

        try {

            // -------------------------------------------------
            // Geocode origin
            // -------------------------------------------------

        	List<Map<String, Object>> originResults =
        	        openStreetMapService.geocodeAddress(originAddress);

            if (originResults == null
                    || originResults.isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Origin location not found: "
                                + originAddress
                );
            }

            Map<String, Object> origin =
                    originResults.get(0);

            double originLat =
                    Double.parseDouble(
                            String.valueOf(
                                    origin.get("lat")
                            )
                    );

            double originLon =
                    Double.parseDouble(
                            String.valueOf(
                                    origin.get("lon")
                            )
                    );

            // -------------------------------------------------
            // Geocode destination
            // -------------------------------------------------

            List<Map<String, Object>> destinationResults =
                    openStreetMapService.geocodeAddress(destinationAddress);

            if (destinationResults == null
                    || destinationResults.isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Destination location not found: "
                                + destinationAddress
                );
            }

            Map<String, Object> destination =
                    destinationResults.get(0);

            double destinationLat =
                    Double.parseDouble(
                            String.valueOf(
                                    destination.get("lat")
                            )
                    );

            double destinationLon =
                    Double.parseDouble(
                            String.valueOf(
                                    destination.get("lon")
                            )
                    );

            // -------------------------------------------------
            // Call OSRM
            // -------------------------------------------------

            Map<String, Object> response =
                    osrmService.getRoute(
                            originLon,
                            originLat,
                            destinationLon,
                            destinationLat
                    );

            if (response == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "No response received from OSRM"
                );
            }

            String code =
                    String.valueOf(
                            response.get("code")
                    );

            if (!"Ok".equalsIgnoreCase(code)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OSRM routing failed: "
                                + code
                );
            }

            List<Map<String, Object>> routes =
                    (List<Map<String, Object>>)
                            response.get("routes");

            if (routes == null
                    || routes.isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "No route found by OSRM"
                );
            }

            Map<String, Object> firstRoute =
                    routes.get(0);

            // -------------------------------------------------
            // Distance
            // -------------------------------------------------

            Number distanceMeters =
                    (Number) firstRoute.get("distance");

            // -------------------------------------------------
            // Duration
            // -------------------------------------------------

            Number durationSeconds =
                    (Number) firstRoute.get("duration");

            if (distanceMeters == null
                    || durationSeconds == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OSRM did not return distance or duration"
                );
            }

            double distanceKm =
                    distanceMeters.doubleValue()
                            / 1000.0;

            int estimatedTimeMinutes =
                    (int) Math.ceil(
                            durationSeconds.doubleValue()
                                    / 60.0
                    );

            return new RouteMetrics(
                    distanceKm,
                    estimatedTimeMinutes
            );

        } catch (ResponseStatusException e) {

            throw e;

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to calculate route using OpenStreetMap/OSRM: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // RESPONSE MAPPING
    // =========================================================

    private RouteResponse mapToResponse(
            Route route) {

        return RouteResponse.builder()
                .id(route.getId())
                .shipmentId(
                        route.getShipment().getId()
                )
                .origin(route.getOrigin())
                .destination(route.getDestination())
                .waypoints(route.getWaypoints())
                .distanceKm(route.getDistanceKm())
                .estimatedTimeMinutes(
                        route.getEstimatedTimeMinutes()
                )
                .actualTimeMinutes(
                        route.getActualTimeMinutes()
                )
                .trafficCondition(
                        route.getTrafficCondition()
                )
                .driverId(
                        route.getDriver() != null
                                ? route.getDriver().getId()
                                : null
                )
                .isCurrent(route.getIsCurrent())
                .status(route.getStatus())
                .build();
    }

    // =========================================================
    // INTERNAL ROUTE METRICS
    // =========================================================

    private record RouteMetrics(
            double distanceKm,
            int estimatedTimeMinutes
    ) {
    }
}