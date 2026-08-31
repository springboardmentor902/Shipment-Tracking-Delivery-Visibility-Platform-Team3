package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.CancelShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.AssignOperatorRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;
import com.shiptrack.shiptrack_pro.dto.ShipmentUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.StatusUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.TrackingEventResponse;
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import com.shiptrack.shiptrack_pro.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final TrackingService trackingService;

    /**
     * Create a shipment.
     * Customers, business clients and logistics operators may book one;
     * support agents and administrators only manage existing shipments.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR')")
    public ResponseEntity<ShipmentResponse> createShipment(@Valid @RequestBody ShipmentRequest request) {
        return new ResponseEntity<>(shipmentService.createShipment(request), HttpStatus.CREATED);
    }

    /**
     * Fetch all shipments visible to the caller, newest first.
     * Optional ?status=IN_TRANSIT filter, plus page/size/sort.
     */
    @GetMapping
    public ResponseEntity<Page<ShipmentResponse>> getShipments(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(shipmentService.getShipments(status, pageable));
    }

    /** Fetch a single shipment by its database id. */
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentById(id));
    }

    @GetMapping("/{id}/tracking")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<TrackingEventResponse>> getShipmentTracking(@PathVariable Long id) {
        return ResponseEntity.ok(trackingService.getShipmentEvents(id));
    }

    /** Fetch a single shipment by its public tracking number. */
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> getShipmentByTrackingNumber(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(shipmentService.getShipmentByTrackingNumber(trackingNumber));
    }

    /** Edit shipment details. Partial payload — only the fields you send are applied. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> updateShipment(@PathVariable Long id,
                                                           @Valid @RequestBody ShipmentUpdateRequest request) {
        return ResponseEntity.ok(shipmentService.updateShipment(id, request));
    }

    /**
     * Move a shipment from one status to the next.
     * Validated against the allowed transition map — illegal jumps return 409.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(shipmentService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/operator")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'LOGISTICS_OPERATOR')")
    public ResponseEntity<ShipmentResponse> assignOperator(@PathVariable Long id,
                                                            @Valid @RequestBody AssignOperatorRequest request) {
        return ResponseEntity.ok(shipmentService.assignOperator(id, request));
    }

    /**
     * Cancel a shipment. Soft cancel — the row is kept and moved to CANCELLED
     * with a timestamp and reason, so analytics and audit history stay intact.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> cancelShipment(@PathVariable Long id,
                                                           @Valid @RequestBody CancelShipmentRequest request) {
        return ResponseEntity.ok(shipmentService.cancelShipment(id, request));
    }
}
