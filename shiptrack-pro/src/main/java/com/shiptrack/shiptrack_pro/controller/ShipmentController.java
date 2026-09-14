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
import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    // =========================================================
    // CREATE SHIPMENT
    // =========================================================

    @PostMapping
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody ShipmentRequest request,
            Authentication authentication) {

        String customerEmail = authentication.getName();

        ShipmentResponse response =
                shipmentService.createShipment(request, customerEmail);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================================================
    // ASSIGN OPERATOR
    // =========================================================

    @PatchMapping("/{trackingNumber}/operator")
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<ShipmentResponse> assignOperator(
            @PathVariable String trackingNumber,
            @RequestParam Long operatorId) {

        return ResponseEntity.ok(
                shipmentService.assignOperator(
                        trackingNumber,
                        operatorId
                )
        );
    }

    // =========================================================
    // UPDATE SHIPMENT STATUS
    // =========================================================

    @PatchMapping("/{trackingNumber}/status")
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable String trackingNumber,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                shipmentService.updateShipmentStatus(
                        trackingNumber,
                        status
                )
        );
    }

    // =========================================================
    // GET SHIPMENTS
    // CUSTOMER / BUSINESS → OWN SHIPMENTS
    // ADMIN → ALL SHIPMENTS
    // =========================================================

    @GetMapping
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<List<ShipmentResponse>> getShipments(
            Authentication authentication) {

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"))) {

            return ResponseEntity.ok(
                    shipmentService.getAllShipments()
            );
        }

        String customerEmail = authentication.getName();

        return ResponseEntity.ok(
                shipmentService.getCustomerShipments(customerEmail)
        );
    }

    // =========================================================
    // GET ONE SHIPMENT BY TRACKING NUMBER
    // CUSTOMER / BUSINESS → OWN SHIPMENT
    // ADMIN → ANY SHIPMENT
    // =========================================================

    @GetMapping("/{trackingNumber}")
    @PreAuthorize("""
    	    hasAnyRole(
    	        'BUSINESS_CLIENT',
    	        'CUSTOMER',
    	        'LOGISTICS_OPERATOR',
    	        'ADMINISTRATOR'
    	    )
    	""")
    public ResponseEntity<ShipmentResponse> getShipmentByTrackingNumber(
            @PathVariable String trackingNumber,
            Authentication authentication) {

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"))) {

            return ResponseEntity.ok(
                    shipmentService.getShipmentByTrackingNumber(trackingNumber)
            );
        }

        String customerEmail = authentication.getName();

        return ResponseEntity.ok(
                shipmentService.getCustomerShipmentByTrackingNumber(
                        trackingNumber,
                        customerEmail
                )
        );
    }
}