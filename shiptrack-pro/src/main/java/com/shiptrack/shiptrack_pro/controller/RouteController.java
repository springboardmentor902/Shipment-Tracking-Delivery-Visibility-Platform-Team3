package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    // Create a route
    @PostMapping
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> createRoute(
            @Valid @RequestBody RouteRequest request) {

        RouteResponse response = routeService.createRoute(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Assign or change driver
    @PatchMapping("/{routeId}/driver")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> assignDriver(
            @PathVariable Long routeId,
            @RequestParam Long driverId) {

        return ResponseEntity.ok(
                routeService.assignDriver(routeId, driverId)
        );
    }

    // Get route by shipment ID
    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> getRouteByShipmentId(
            @PathVariable Long shipmentId) {

        return ResponseEntity.ok(
                routeService.getRouteByShipmentId(shipmentId)
        );
    }
}