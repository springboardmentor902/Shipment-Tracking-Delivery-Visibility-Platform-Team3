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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ETAPredictionServiceImpl
        implements ETAPredictionService {

    private final ETAPredictionRepository etaPredictionRepository;

    private final RouteRepository routeRepository;

    private final ShipmentRepository shipmentRepository;


    // =========================================================
    // Get At-Risk Predictions
    // =========================================================

    @Override
    public List<ETAPredictionResponse> getAtRiskPredictions(
            double minScore) {

        List<ETAPrediction> allPredictions =
                etaPredictionRepository.findAll();

        allPredictions.forEach(prediction -> {

            if (prediction.getShipment() != null) {

                System.out.println(
                        "ETA ID: " + prediction.getId()
                                + ", Shipment ID: "
                                + prediction.getShipment().getId()
                                + ", Risk Score: "
                                + prediction.getDelayRiskScore()
                                + ", Requested Minimum: "
                                + minScore
                );

            } else {

                System.out.println(
                        "ETA ID: " + prediction.getId()
                                + ", Shipment ID: null"
                                + ", Risk Score: "
                                + prediction.getDelayRiskScore()
                                + ", Requested Minimum: "
                                + minScore
                );
            }
        });

        return allPredictions
                .stream()
                .filter(prediction ->
                        prediction.getDelayRiskScore() != null
                                && prediction.getDelayRiskScore() >= minScore
                )
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // Predict ETA
    // =========================================================

    @Override
    public ETAPredictionResponse predictETA(
            ETAPredictionRequest request) {

        // -----------------------------------------------------
        // Validate request
        // -----------------------------------------------------

        if (request == null
                || request.getShipmentId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Shipment ID is required"
            );
        }

        Long shipmentId = request.getShipmentId();


        // -----------------------------------------------------
        // Find shipment
        // -----------------------------------------------------

        Shipment shipment = shipmentRepository
                .findById(shipmentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Shipment not found with id: "
                                        + shipmentId
                        )
                );


        // -----------------------------------------------------
        // Find current/latest route
        // -----------------------------------------------------

        List<Route> routes =
                routeRepository
                        .findAllByShipmentIdOrderByCreatedAtDesc(
                                shipmentId
                        );

        if (routes.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Route not found for shipment id: "
                            + shipmentId
            );
        }

        Route route = routes
                .stream()
                .filter(r ->
                        Boolean.TRUE.equals(r.getIsCurrent())
                )
                .findFirst()
                .orElse(routes.get(0));


        // -----------------------------------------------------
        // Get estimated travel time
        // -----------------------------------------------------

        Integer estimatedTimeMinutes =
                route.getEstimatedTimeMinutes();

        if (estimatedTimeMinutes == null
                || estimatedTimeMinutes <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estimated travel time is not available for this route"
            );
        }


        // =====================================================
        // Traffic adjustment
        // =====================================================

        double trafficMultiplier = 1.0;

        String trafficCondition =
                route.getTrafficCondition();

        if (trafficCondition != null
                && !trafficCondition.isBlank()) {

            switch (
                    trafficCondition
                            .trim()
                            .toUpperCase()
            ) {

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
                    break;
            }
        }


        // =====================================================
        // Calculate adjusted travel time
        // =====================================================

        int adjustedTimeMinutes =
                (int) Math.ceil(
                        estimatedTimeMinutes
                                * trafficMultiplier
                );


        // =====================================================
        // Predicted delivery time
        // =====================================================

        LocalDateTime now =
                LocalDateTime.now();

        LocalDateTime predictedDeliveryTime =
                now.plusMinutes(adjustedTimeMinutes);


        // =====================================================
        // Delay risk score
        // =====================================================

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


        // =====================================================
        // Confidence score
        // =====================================================

        double confidenceScore = 80.0;

        if (route.getDistanceKm() == null) {

            confidenceScore -= 10.0;
        }

        if (trafficCondition == null
                || trafficCondition.isBlank()) {

            confidenceScore -= 10.0;
        }


        // =====================================================
        // Explanation
        // =====================================================

        String factors =
                "Base travel time: "
                        + estimatedTimeMinutes
                        + " minutes; "
                        + "Traffic condition: "
                        + (
                                trafficCondition != null
                                        && !trafficCondition.isBlank()
                                        ? trafficCondition
                                        : "Unknown"
                        )
                        + "; Traffic multiplier: "
                        + trafficMultiplier;


        // =====================================================
        // Find latest existing ETA prediction
        // =====================================================

        List<ETAPrediction> predictions =
                etaPredictionRepository
                        .findByShipmentIdOrderByCalculatedAtDesc(
                                shipmentId
                        );

        ETAPrediction prediction =
                predictions
                        .stream()
                        .findFirst()
                        .orElse(null);


        // =====================================================
        // Create prediction if none exists
        // =====================================================

        if (prediction == null) {

            prediction = ETAPrediction
                    .builder()
                    .shipment(shipment)
                    .build();
        }


        // =====================================================
        // Update prediction
        // =====================================================

        prediction.setPredictedDeliveryTime(
                predictedDeliveryTime
        );

        prediction.setDelayRiskScore(
                delayRiskScore
        );

        prediction.setConfidenceScore(
                confidenceScore
        );

        prediction.setFactors(
                factors
        );

        prediction.setCalculatedAt(
                now
        );


        // =====================================================
        // Save prediction
        // =====================================================

        ETAPrediction savedPrediction =
                etaPredictionRepository.save(
                        prediction
                );

        return mapToResponse(savedPrediction);
    }


    // =========================================================
    // Automatic ETA Recalculation
    // =========================================================

    @Override
    public void recalculateETA(Long shipmentId) {

        if (shipmentId == null) {
            return;
        }

        ETAPredictionRequest request =
                new ETAPredictionRequest();

        request.setShipmentId(shipmentId);

        predictETA(request);
    }


    // =========================================================
    // Get ETA By Shipment ID
    // =========================================================

    @Override
    public ETAPredictionResponse getETAByShipmentId(
            Long shipmentId) {

        if (shipmentId == null) {

            throw new IllegalArgumentException(
                    "Shipment ID cannot be null"
            );
        }

        List<ETAPrediction> predictions =
                etaPredictionRepository
                        .findByShipmentIdOrderByCalculatedAtDesc(
                                shipmentId
                        );

        ETAPrediction prediction =
                predictions
                        .stream()
                        .findFirst()
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "No ETA prediction found for shipment ID: "
                                                + shipmentId
                                )
                        );

        return mapToResponse(prediction);
    }


    // =========================================================
    // Map Entity To Response
    // =========================================================

    private ETAPredictionResponse mapToResponse(
            ETAPrediction prediction) {

        Shipment shipment =
                prediction.getShipment();

        Integer expectedDelayMinutes =
                null;

       
        return ETAPredictionResponse
                .builder()

                .id(
                        prediction.getId()
                )

                .shipmentId(
                        shipment != null
                                ? shipment.getId()
                                : null
                )

                .trackingNumber(
                        shipment != null
                                ? shipment.getTrackingNumber()
                                : null
                )

                .receiverName(
                        shipment != null
                                ? shipment.getReceiverName()
                                : null
                )

                .status(
                        shipment != null
                                && shipment.getStatus() != null
                                        ? shipment.getStatus().toString()
                                        : null
                )

                .promisedDeliveryTime(null)
                

                .predictedDeliveryTime(
                        prediction.getPredictedDeliveryTime()
                )

                .calculatedAt(
                        prediction.getCalculatedAt()
                )

                .confidenceScore(
                        prediction.getConfidenceScore()
                )

                .delayRiskScore(
                        prediction.getDelayRiskScore()
                )

                .expectedDelayMinutes(
                        expectedDelayMinutes
                )

                .factors(
                        prediction.getFactors()
                )

                .build();
    }
}