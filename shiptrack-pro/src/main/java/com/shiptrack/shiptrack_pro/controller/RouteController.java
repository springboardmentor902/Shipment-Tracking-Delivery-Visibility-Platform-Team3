package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.dto.RouteUpdateRequest;
import com.shiptrack.shiptrack_pro.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /** Create one route leg for a shipment. Operator/admin only. */
    @PostMapping
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody RouteRequest request) {
        return new ResponseEntity<>(routeService.createRoute(request), HttpStatus.CREATED);
    }

    /** Update a leg or reassign its driver. Operator/admin only. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> updateRoute(@PathVariable Long id,
                                                     @Valid @RequestBody RouteUpdateRequest request) {
        return ResponseEntity.ok(routeService.updateRoute(id, request));
    }

    /**
     * Recalculate a leg's coordinates, distance, duration and live traffic from
     * Google Maps. Safe to call without a Maps key: the leg is returned unchanged.
     */
    @PostMapping("/{id}/refresh")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> refreshRoute(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.refreshRouteFromMaps(id));
    }

    /**
     * All legs of a shipment, in travel order.
     * Any authenticated user who is allowed to see the shipment can read them,
     * but only operators/admins can change them.
     */
    @GetMapping("/{shipmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RouteResponse>> getRoutes(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(routeService.getRoutes(shipmentId));
    }

    /** A single leg by its own id. */
    @GetMapping("/leg/{routeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RouteResponse> getRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(routeService.getRoute(routeId));
    }
}
