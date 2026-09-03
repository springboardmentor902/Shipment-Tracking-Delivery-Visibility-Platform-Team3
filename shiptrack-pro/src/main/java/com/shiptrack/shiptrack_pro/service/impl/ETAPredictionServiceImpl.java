package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.ETAPredictionRequest;
import com.shiptrack.shiptrack_pro.dto.ETAPredictionResponse;
import com.shiptrack.shiptrack_pro.entity.ETAPrediction;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.repository.ETAPredictionRepository;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.service.ETAPredictionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ETAPredictionServiceImpl implements ETAPredictionService {

    private final ETAPredictionRepository etaPredictionRepository;
    private final RouteRepository routeRepository;
    private final ShipmentRepository shipmentRepository;

    @Override
    public ETAPredictionResponse predictETA(
            ETAPredictionRequest request) {

        Shipment shipment = shipmentRepository
                .findById(request.getShipmentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found with id: "
                                + request.getShipmentId()
                ));

        Route route = routeRepository
                .findByShipmentId(request.getShipmentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Route not found for shipment id: "
                                + request.getShipmentId()
                ));

        Integer estimatedTimeMinutes =
                route.getEstimatedTimeMinutes();

        if (estimatedTimeMinutes == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estimated travel time is not available for this route"
            );
        }

        // =========================
        // Traffic adjustment
        // =========================

        double trafficMultiplier = 1.0;

        String trafficCondition =
                route.getTrafficCondition();

        if (trafficCondition != null) {

            switch (trafficCondition.toUpperCase()) {

                case "LOW":
                case "LIGHT":
                    trafficMultiplier = 1.0;
                    break;

                case "MODERATE":
                    trafficMultiplier = 1.20;
                    break;

                case "HIGH":
                case "HEAVY":
                    trafficMultiplier = 1.50;
                    break;

                case "SEVERE":
                    trafficMultiplier = 1.80;
                    break;

                default:
                    trafficMultiplier = 1.0;
            }
        }

        // =========================
        // Calculate travel time
        // =========================

        int adjustedTimeMinutes =
                (int) Math.ceil(
                        estimatedTimeMinutes * trafficMultiplier
                );

        // =========================
        // Predicted delivery time
        // =========================

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime predictedDeliveryTime =
                now.plusMinutes(adjustedTimeMinutes);

        // =========================
        // Delay risk score
        // =========================

        double delayRiskScore;

        if (trafficMultiplier >= 1.80) {
            delayRiskScore = 9.0;

        } else if (trafficMultiplier >= 1.50) {
            delayRiskScore = 7.0;

        } else if (trafficMultiplier >= 1.20) {
            delayRiskScore = 4.0;

        } else {
            delayRiskScore = 1.0;
        }

        // =========================
        // Confidence score
        // =========================

        double confidenceScore = 80.0;

        if (route.getDistanceKm() == null) {
            confidenceScore -= 10.0;
        }

        if (trafficCondition == null
                || trafficCondition.isBlank()) {
            confidenceScore -= 10.0;
        }

        // =========================
        // Explanation
        // =========================

        String factors =
                "Base travel time: "
                        + estimatedTimeMinutes
                        + " minutes; "
                        + "Traffic condition: "
                        + (trafficCondition != null
                                ? trafficCondition
                                : "Unknown")
                        + "; Traffic multiplier: "
                        + trafficMultiplier;

        // =========================
        // Find existing prediction
        // =========================

        ETAPrediction prediction =
                etaPredictionRepository
                        .findByShipmentId(request.getShipmentId())
                        .orElse(null);

        // If no prediction exists, create one
        if (prediction == null) {

            prediction = ETAPrediction.builder()
                    .shipment(shipment)
                    .build();
        }

        // =========================
        // Update prediction
        // =========================

        prediction.setPredictedDeliveryTime(
                predictedDeliveryTime
        );

        prediction.setDelayRiskScore(
                delayRiskScore
        );

        prediction.setConfidenceScore(
                confidenceScore
        );

        prediction.setFactors(factors);

        prediction.setCalculatedAt(now);

        // =========================
        // Save prediction
        // =========================

        ETAPrediction savedPrediction =
                etaPredictionRepository.save(prediction);

        return mapToResponse(savedPrediction);
    }

    // ==========================================
    // Automatic ETA recalculation
    // ==========================================

    @Override
    public void recalculateETA(Long shipmentId) {

        ETAPredictionRequest request =
                new ETAPredictionRequest();

        request.setShipmentId(shipmentId);

        predictETA(request);
    }

    // ==========================================
    // Get current ETA
    // ==========================================

    @Override
    public ETAPredictionResponse getETAByShipmentId(
            Long shipmentId) {

        ETAPrediction prediction =
                etaPredictionRepository
                        .findByShipmentId(shipmentId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "ETA prediction not found for shipment id: "
                                                + shipmentId
                                ));

        return mapToResponse(prediction);
    }

    // ==========================================
    // Map entity to response
    // ==========================================

    private ETAPredictionResponse mapToResponse(
            ETAPrediction prediction) {

        return ETAPredictionResponse.builder()
                .id(prediction.getId())
                .shipmentId(
                        prediction.getShipment().getId()
                )
                .predictedDeliveryTime(
                        prediction.getPredictedDeliveryTime()
                )
                .delayRiskScore(
                        prediction.getDelayRiskScore()
                )
                .confidenceScore(
                        prediction.getConfidenceScore()
                )
                .factors(
                        prediction.getFactors()
                )
                .calculatedAt(
                        prediction.getCalculatedAt()
                )
                .build();
    }
}