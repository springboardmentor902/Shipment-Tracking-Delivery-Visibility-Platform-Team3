package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.LocationUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.TrackingEventResponse;
import com.shiptrack.shiptrack_pro.dto.TrackingResponse;
import com.shiptrack.shiptrack_pro.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<TrackingResponse> getTracking(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(trackingService.getTracking(trackingNumber));
    }

    // operator pushes his location while delivering
    @PostMapping("/location")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<TrackingEventResponse> updateLocation(@Valid @RequestBody LocationUpdateRequest request) {
        return ResponseEntity.ok(trackingService.recordLocation(request));
    }
}
