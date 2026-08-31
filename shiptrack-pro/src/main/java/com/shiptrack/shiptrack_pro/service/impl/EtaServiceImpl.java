package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.EtaResponse;
import com.shiptrack.shiptrack_pro.entity.DelayRiskLevel;
import com.shiptrack.shiptrack_pro.entity.DeliveryRoute;
import com.shiptrack.shiptrack_pro.entity.EtaPrediction;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.DeliveryRouteRepository;
import com.shiptrack.shiptrack_pro.repository.EtaPredictionRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.ShipmentAccessPolicy;
import com.shiptrack.shiptrack_pro.service.EtaCalculator;
import com.shiptrack.shiptrack_pro.service.EtaService;
import com.shiptrack.shiptrack_pro.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Stores and serves delivery forecasts.
 *
 * The arithmetic lives in {@link EtaCalculator}; this class only gathers the
 * inputs, persists the single row per shipment, and enforces who may read what.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EtaServiceImpl implements EtaService {

    /** Shipments worth recalculating on a schedule. */
    private static final Set<ShipmentStatus> ACTIVE_STATUSES = Set.of(
            ShipmentStatus.CREATED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.FAILED_DELIVERY);

    private static final String FACTOR_SEPARATOR = "\n";

    private final ShipmentRepository shipmentRepository;
    private final DeliveryRouteRepository routeRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final EtaPredictionRepository etaPredictionRepository;
    private final CurrentUserService currentUserService;
    private final ShipmentAccessPolicy accessPolicy;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public EtaResponse recalculate(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
        return recalculate(shipment);
    }

    @Override
    @Transactional
    public EtaResponse recalculate(Shipment shipment) {
        List<DeliveryRoute> legs = routeRepository.findByShipmentIdOrderByLegNumberAsc(shipment.getId());
        TrackingEvent lastEvent = trackingEventRepository
                .findFirstByShipmentOrderByRecordedAtDesc(shipment)
                .orElse(null);

        EtaCalculator.Result result = EtaCalculator.calculate(shipment, legs, lastEvent, LocalDateTime.now());

        EtaPrediction prediction = etaPredictionRepository.findByShipmentId(shipment.getId())
                .orElseGet(() -> EtaPrediction.builder().shipment(shipment).build());

        // remembered before the overwrite, so an alert only fires when risk worsens
        DelayRiskLevel previousLevel = prediction.getRiskLevel();

        prediction.setPredictedDeliveryAt(result.predictedDeliveryAt());
        prediction.setPromisedDeliveryDate(result.promisedDeliveryDate());
        prediction.setExpectedDelayMinutes(result.expectedDelayMinutes());
        prediction.setDelayRiskScore(result.delayRiskScore());
        prediction.setRiskLevel(result.riskLevel());
        prediction.setConfidenceScore(result.confidenceScore());
        prediction.setFactors(truncate(String.join(FACTOR_SEPARATOR, result.factors())));
        prediction.setSource(result.source());
        prediction.setCalculatedAt(LocalDateTime.now());

        EtaPrediction saved = etaPredictionRepository.save(prediction);
        alertIfRiskWorsened(shipment, previousLevel, saved);
        return toResponse(shipment, saved);
    }

    /**
     * Delay alerts fire on escalation only.
     *
     * The forecast is recalculated on every location ping, so alerting on the
     * current level alone would send the same warning dozens of times. A repeat at
     * the same level is left to the notification cooldown; a drop in risk is not
     * news at all.
     */
    private void alertIfRiskWorsened(Shipment shipment, DelayRiskLevel previousLevel,
                                     EtaPrediction prediction) {
        DelayRiskLevel level = prediction.getRiskLevel();
        if (level == null || !ACTIVE_STATUSES.contains(shipment.getStatus())) {
            return;
        }
        // below HIGH is worth showing on the dashboard but not worth an alert
        if (level.ordinal() < DelayRiskLevel.HIGH.ordinal()) {
            return;
        }
        if (previousLevel != null && level.ordinal() <= previousLevel.ordinal()) {
            return;
        }
        notificationService.notifyDelayRisk(shipment, level,
                prediction.getDelayRiskScore() == null ? 0 : prediction.getDelayRiskScore(),
                prediction.getPredictedDeliveryAt(), prediction.getExpectedDelayMinutes());
    }

    /**
     * Used by the tracking and route write paths. A forecast is a convenience,
     * never a reason to fail the write that produced it.
     */
    @Override
    public void refreshQuietly(Long shipmentId) {
        try {
            recalculate(shipmentId);
        } catch (RuntimeException ex) {
            log.warn("Could not refresh the ETA for shipment {}: {}", shipmentId, ex.getMessage());
        }
    }

    @Override
    @Transactional
    public EtaResponse getForShipment(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));

        User actor = currentUserService.getCurrentUser();
        if (!accessPolicy.canView(shipment, actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have access to this shipment");
        }

        // No forecast stored yet (older shipment, or the write-path refresh
        // failed) — produce one now rather than showing the customer nothing.
        return etaPredictionRepository.findByShipmentId(shipmentId)
                .map(prediction -> toResponse(shipment, prediction))
                .orElseGet(() -> recalculate(shipment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EtaResponse> getAtRisk(int minimumScore) {
        User actor = currentUserService.getCurrentUser();
        int floor = Math.max(0, Math.min(100, minimumScore));

        return etaPredictionRepository
                .findByDelayRiskScoreGreaterThanEqualOrderByDelayRiskScoreDesc(floor).stream()
                .filter(prediction -> {
                    Shipment shipment = prediction.getShipment();
                    return shipment != null
                            && shipment.getStatus() != ShipmentStatus.DELIVERED
                            && shipment.getStatus() != ShipmentStatus.CANCELLED
                            && accessPolicy.canView(shipment, actor);
                })
                .map(prediction -> toResponse(prediction.getShipment(), prediction))
                .toList();
    }

    @Override
    @Transactional
    public int recalculateActive() {
        List<Shipment> active = shipmentRepository.findByStatusIn(ACTIVE_STATUSES);
        int updated = 0;
        for (Shipment shipment : active) {
            try {
                recalculate(shipment);
                updated++;
            } catch (RuntimeException ex) {
                log.warn("Scheduled ETA refresh failed for shipment {}: {}",
                        shipment.getId(), ex.getMessage());
            }
        }
        return updated;
    }

    private EtaResponse toResponse(Shipment shipment, EtaPrediction prediction) {
        return EtaResponse.builder()
                .shipmentId(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus() != null ? shipment.getStatus().name() : null)
                .priority(shipment.getPriority() != null ? shipment.getPriority().name() : null)
                .receiverName(shipment.getReceiverName())
                .deliveryAddress(shipment.getDeliveryAddress())
                .assignedOperatorName(shipment.getAssignedOperator() != null
                        ? shipment.getAssignedOperator().getFullName() : null)
                .predictedDeliveryAt(prediction.getPredictedDeliveryAt())
                .promisedDeliveryDate(prediction.getPromisedDeliveryDate())
                .expectedDelayMinutes(prediction.getExpectedDelayMinutes())
                .delayRiskScore(prediction.getDelayRiskScore())
                .riskLevel(prediction.getRiskLevel() != null ? prediction.getRiskLevel().name() : null)
                .confidenceScore(prediction.getConfidenceScore())
                .factors(splitFactors(prediction.getFactors()))
                .source(prediction.getSource())
                .calculatedAt(prediction.getCalculatedAt())
                .build();
    }

    private static List<String> splitFactors(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return Arrays.stream(stored.split(FACTOR_SEPARATOR))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 997) + "...";
    }
}
