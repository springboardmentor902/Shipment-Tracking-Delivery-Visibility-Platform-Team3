package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.service.ShipmentService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final ShipmentService shipmentService;
    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<Map<String, Object>> trackShipment(
            @PathVariable String trackingNumber) {

        // Get shipment details
        Shipment shipment = shipmentRepository
                .findByTrackingNumber(trackingNumber)
                .orElseThrow(() ->
                        new RuntimeException("Shipment not found"));

        ShipmentResponse shipmentResponse =
                shipmentService.getShipmentByTrackingNumber(trackingNumber);

        // Get tracking events
        List<TrackingEvent> trackingEvents =
                trackingEventRepository
                        .findByShipmentIdOrderByEventTimeAsc(
                                shipment.getId());

        // Convert events to frontend-friendly response
        List<Map<String, Object>> events = trackingEvents.stream()
                .map(event -> {
                    Map<String, Object> item = new LinkedHashMap<>();

                    item.put("id", event.getId());
                    item.put("status", event.getStatus());
                    item.put("recordedAt", event.getEventTime());

                    if (event.getLatitude() != null &&
                        event.getLongitude() != null) {

                        item.put(
                                "location",
                                event.getLatitude() + ", "
                                        + event.getLongitude()
                        );
                    } else {
                        item.put("location", null);
                    }

                    // These fields do not exist in TrackingEvent
                    item.put("notes", null);
                    item.put("recordedByName", "System");

                    return item;
                })
                .toList();

        // Response expected by Tracking.jsx
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("shipment", shipmentResponse);
        response.put("events", events);

        return ResponseEntity.ok(response);
    }
}