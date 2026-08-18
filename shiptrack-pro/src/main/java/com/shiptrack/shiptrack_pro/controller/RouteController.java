package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> saveRoute(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(routeService.saveRoute(request));
    }

    @GetMapping("/{shipmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RouteResponse> getRoute(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(routeService.getRoute(shipmentId));
    }
}
