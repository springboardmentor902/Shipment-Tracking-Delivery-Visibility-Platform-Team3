package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.ETAPredictionRequest;
import com.shiptrack.shiptrack_pro.dto.ETAPredictionResponse;
import com.shiptrack.shiptrack_pro.service.ETAPredictionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eta")
@RequiredArgsConstructor
public class ETAPredictionController {

    private final ETAPredictionService etaPredictionService;

    // Predict ETA for a shipment
    @PostMapping("/{shipmentId}/predict")
    public ResponseEntity<ETAPredictionResponse> predictETA(
            @PathVariable Long shipmentId) {

        ETAPredictionRequest request = new ETAPredictionRequest();
        request.setShipmentId(shipmentId);

        ETAPredictionResponse response =
                etaPredictionService.predictETA(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get ETA prediction for a shipment
    @GetMapping("/{shipmentId}")
    public ResponseEntity<ETAPredictionResponse> getETA(
            @PathVariable Long shipmentId) {

        return ResponseEntity.ok(
                etaPredictionService.getETAByShipmentId(shipmentId)
        );
    }
}