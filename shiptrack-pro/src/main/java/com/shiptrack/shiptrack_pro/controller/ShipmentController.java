package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    // =========================================================
    // CREATE SHIPMENT
    // POST /api/shipments
    // =========================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT')")
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody ShipmentRequest request,
            Authentication authentication) {

        String customerEmail = authentication.getName();

        ShipmentResponse response =
                shipmentService.createShipment(
                        request,
                        customerEmail
                );

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }


    // =========================================================
    // GET ALL SHIPMENTS OF LOGGED-IN CUSTOMER
    // GET /api/shipments
    // =========================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT')")
    public ResponseEntity<List<ShipmentResponse>> getCustomerShipments(
            Authentication authentication) {

        String customerEmail = authentication.getName();

        List<ShipmentResponse> shipments =
                shipmentService.getCustomerShipments(
                        customerEmail
                );

        return ResponseEntity.ok(shipments);
    }


    // =========================================================
    // GET ONE SHIPMENT BY TRACKING NUMBER
    // GET /api/shipments/{trackingNumber}
    // =========================================================

    @GetMapping("/{trackingNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT')")
    public ResponseEntity<ShipmentResponse> getCustomerShipmentByTrackingNumber(
            @PathVariable String trackingNumber,
            Authentication authentication) {

        String customerEmail = authentication.getName();

        ShipmentResponse response =
                shipmentService.getCustomerShipmentByTrackingNumber(
                        trackingNumber,
                        customerEmail
                );

        return ResponseEntity.ok(response);
    }
}
