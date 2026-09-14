package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.ETAPredictionRequest;
import com.shiptrack.shiptrack_pro.dto.ETAPredictionResponse;
import com.shiptrack.shiptrack_pro.service.ETAPredictionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eta")
@RequiredArgsConstructor
public class ETAPredictionController {

    private final ETAPredictionService etaPredictionService;

    // =========================================================
    // PREDICT / RECALCULATE ETA
    // =========================================================

    @PostMapping("/{shipmentId}/predict")
    public ResponseEntity<ETAPredictionResponse> predictETA(
            @PathVariable Long shipmentId) {

        ETAPredictionRequest request =
                new ETAPredictionRequest();

        request.setShipmentId(shipmentId);

        ETAPredictionResponse response =
                etaPredictionService.predictETA(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // GET ETA FOR ONE SHIPMENT
    // =========================================================

    @GetMapping("/{shipmentId}")
    public ResponseEntity<ETAPredictionResponse> getETA(
            @PathVariable Long shipmentId) {

        return ResponseEntity.ok(
                etaPredictionService
                        .getETAByShipmentId(shipmentId)
        );
    }

    // =========================================================
    // GET SHIPMENTS AT RISK
    // =========================================================

    @GetMapping("/at-risk")
    public ResponseEntity<List<ETAPredictionResponse>> getAtRisk(
            @RequestParam(defaultValue = "5") double minScore) {

        return ResponseEntity.ok(
                etaPredictionService
                        .getAtRiskPredictions(minScore)
        );
    }
}