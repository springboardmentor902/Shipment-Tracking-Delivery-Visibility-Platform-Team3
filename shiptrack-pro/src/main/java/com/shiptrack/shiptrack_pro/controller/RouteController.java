package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.LocationUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
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

    @PostMapping
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<RouteResponse> createRoute(
            @Valid @RequestBody RouteRequest request) {

        RouteResponse response =
                routeService.createRoute(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{routeId}/driver")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> assignDriver(
            @PathVariable Long routeId,
            @RequestParam Long driverId) {

        return ResponseEntity.ok(
                routeService.assignDriver(routeId, driverId)
        );
    }

    @PatchMapping("/{routeId}/status")
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<RouteResponse> updateRouteStatus(
            @PathVariable Long routeId,
            @RequestParam String status) {

        return ResponseEntity.ok(
                routeService.updateRouteStatus(routeId, status)
        );
    }

    @PostMapping("/{routeId}/location")
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<RouteResponse> updateRouteLocation(
            @PathVariable Long routeId,
            @Valid @RequestBody LocationUpdateRequest request) {

        return ResponseEntity.ok(
                routeService.updateRouteLocation(routeId, request)
        );
    }

    @PostMapping("/{routeId}/refresh")
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<RouteResponse> refreshRouteFromMaps(
            @PathVariable Long routeId) {

        return ResponseEntity.ok(
                routeService.refreshRouteFromMaps(routeId)
        );
    }

    @GetMapping("/{shipmentId}")
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<RouteResponse> getRouteByShipmentId(
            @PathVariable Long shipmentId) {

        return ResponseEntity.ok(
                routeService.getRouteByShipmentId(shipmentId)
        );
    }

    @GetMapping("/{shipmentId}/history")
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<List<RouteResponse>> getRouteHistory(
            @PathVariable Long shipmentId) {

        return ResponseEntity.ok(
                routeService.getRouteHistory(shipmentId)
        );
    }
}