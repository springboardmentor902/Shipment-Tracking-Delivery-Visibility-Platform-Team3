package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.LocationRequest;
import com.shiptrack.shiptrack_pro.service.RouteLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteLocationController {

    private final RouteLocationService routeLocationService;

    @PostMapping("/{id}/location")
    public ResponseEntity<Void> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequest request) {

        routeLocationService.updateLocation(id, request);

        return ResponseEntity.ok().build();
    }
}