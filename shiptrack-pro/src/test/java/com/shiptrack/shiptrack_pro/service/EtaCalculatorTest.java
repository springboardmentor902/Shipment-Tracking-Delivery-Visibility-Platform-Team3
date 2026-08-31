package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.entity.DelayRiskLevel;
import com.shiptrack.shiptrack_pro.entity.DeliveryRoute;
import com.shiptrack.shiptrack_pro.entity.RouteLegStatus;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The forecast arithmetic. `now` is injected, so these assertions do not drift
 * with the wall clock.
 */
class EtaCalculatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 9, 0);

    private Shipment shipment(ShipmentStatus status, LocalDate promised) {
        return Shipment.builder()
                .id(7L)
                .trackingNumber("STP1234567890")
                .status(status)
                .priority(ShipmentPriority.STANDARD)
                .estimatedDeliveryDate(promised)
                .build();
    }

    private DeliveryRoute leg(int number, RouteLegStatus status, Integer minutes,
                              Integer trafficMinutes, String traffic, String metricsSource) {
        return DeliveryRoute.builder()
                .id((long) number)
                .legNumber(number)
                .status(status)
                .expectedDurationMinutes(minutes)
                .durationInTrafficMinutes(trafficMinutes)
                .trafficCondition(traffic)
                .metricsSource(metricsSource)
                .build();
    }

    private TrackingEvent event(LocalDateTime at) {
        return TrackingEvent.builder().id(1L).recordedAt(at).build();
    }

    @Test
    @DisplayName("route legs drive the estimate and traffic-aware minutes win")
    void usesTrafficAwareLegDurations() {
        // 120 min planned but 180 in traffic; the forecast must use 180
        List<DeliveryRoute> legs = List.of(leg(1, RouteLegStatus.ACTIVE, 120, 180, "MODERATE", "LIVE_MAPS"));

        EtaCalculator.Result result = EtaCalculator.calculate(
                shipment(ShipmentStatus.IN_TRANSIT, LocalDate.of(2026, 3, 12)),
                legs, event(NOW.minusMinutes(10)), NOW);

        assertEquals(NOW.plusMinutes(180), result.predictedDeliveryAt());
        assertEquals(EtaCalculator.SOURCE_ROUTE_METRICS, result.source());
        assertEquals(90, result.confidenceScore(), "live maps metrics on every leg");
        assertEquals(DelayRiskLevel.LOW, result.riskLevel());
    }

    @Test
    @DisplayName("completed legs are not counted again")
    void skipsCompletedLegs() {
        List<DeliveryRoute> legs = List.of(
                leg(1, RouteLegStatus.COMPLETED, 600, 600, "LIGHT", "LIVE_MAPS"),
                leg(2, RouteLegStatus.ACTIVE, 60, null, "LIGHT", "LIVE_MAPS"));

        EtaCalculator.Result result = EtaCalculator.calculate(
                shipment(ShipmentStatus.IN_TRANSIT, LocalDate.of(2026, 3, 12)),
                legs, event(NOW.minusMinutes(5)), NOW);

        assertEquals(NOW.plusMinutes(60), result.predictedDeliveryAt());
    }

    @Test
    @DisplayName("severe traffic stretches the remaining time and raises the risk")
    void severeTrafficRaisesRisk() {
        List<DeliveryRoute> legs = List.of(leg(1, RouteLegStatus.ACTIVE, 100, 100, "SEVERE", "LIVE_MAPS"));

        EtaCalculator.Result result = EtaCalculator.calculate(
                shipment(ShipmentStatus.IN_TRANSIT, LocalDate.of(2026, 3, 12)),
                legs, event(NOW.minusMinutes(5)), NOW);

        assertEquals(NOW.plusMinutes(130), result.predictedDeliveryAt(), "100 min x 1.30");
        assertTrue(result.delayRiskScore() >= 20, "severe traffic must add risk");
        assertTrue(result.factors().stream().anyMatch(f -> f.toLowerCase().contains("severe traffic")));
    }

    @Test
    @DisplayName("arriving past the promised date produces a positive delay and real risk")
    void latePredictionIsFlagged() {
        // one long leg: 3 days of travel against a promise of tomorrow
        List<DeliveryRoute> legs = List.of(leg(1, RouteLegStatus.ACTIVE, 3 * 24 * 60, null, "LIGHT", "LIVE_MAPS"));

        EtaCalculator.Result result = EtaCalculator.calculate(
                shipment(ShipmentStatus.IN_TRANSIT, LocalDate.of(2026, 3, 11)),
                legs, event(NOW.minusMinutes(5)), NOW);

        assertNotNull(result.expectedDelayMinutes());
        assertTrue(result.expectedDelayMinutes() > 0, "should be late");
        assertTrue(result.delayRiskScore() >= 25);
        assertNotEquals(DelayRiskLevel.LOW, result.riskLevel());
    }

    @Test
    @DisplayName("a promised date already in the past pushes the risk high")
    void overduePromiseIsCritical() {
        List<DeliveryRoute> legs = List.of(leg(1, RouteLegStatus.ACTIVE, 24 * 60, null, "LIGHT", "LIVE_MAPS"));

        EtaCalculator.Result result = EtaCalculator.calculate(
                shipment(ShipmentStatus.IN_TRANSIT, LocalDate.of(2026, 3, 8)),
                legs, event(NOW.minusMinutes(30)), NOW);

        assertTrue(result.delayRiskScore() >= 50, "overdue plus late arrival");
        assertTrue(result.factors().stream()
                .anyMatch(f -> f.contains("promised delivery date has already passed")));
    }

    @Test
    @DisplayName("silence on a moving shipment adds risk and lowers confidence")
    void staleTrackingIsPenalised() {
        List<DeliveryRoute> legs = List.of(leg(1, RouteLegStatus.ACTIVE, 60, null, "LIGHT", "LIVE_MAPS"));
        Shipment moving = shipment(ShipmentStatus.IN_TRANSIT, LocalDate.of(2026, 3, 12));

        EtaCalculator.Result fresh = EtaCalculator.calculate(moving, legs, event(NOW.minusMinutes(10)), NOW);
        EtaCalculator.Result quiet = EtaCalculator.calculate(moving, legs, event(NOW.minusHours(30)), NOW);

        assertTrue(quiet.delayRiskScore() > fresh.delayRiskScore());
        assertTrue(quiet.confidenceScore() < fresh.confidenceScore());
        assertTrue(quiet.factors().stream().anyMatch(f -> f.contains("No tracking update")));
    }

    @Test
    @DisplayName("no route yet falls back to the status guess with low confidence")
    void fallsBackToStatusHeuristic() {
        EtaCalculator.Result result = EtaCalculator.calculate(
                shipment(ShipmentStatus.CREATED, LocalDate.of(2026, 3, 20)),
                List.of(), null, NOW);

        assertEquals(EtaCalculator.SOURCE_STATUS_HEURISTIC, result.source());
        assertEquals(NOW.plusMinutes(3 * 24 * 60), result.predictedDeliveryAt());
        assertTrue(result.confidenceScore() <= 50, "a guess must not look certain");
    }

    @Test
    @DisplayName("a failed attempt is treated as a serious risk")
    void failedDeliveryRaisesRisk() {
        EtaCalculator.Result result = EtaCalculator.calculate(
                shipment(ShipmentStatus.FAILED_DELIVERY, LocalDate.of(2026, 3, 12)),
                List.of(), null, NOW);

        assertTrue(result.delayRiskScore() >= 35);
        assertTrue(result.factors().stream().anyMatch(f -> f.contains("delivery attempt already failed")));
    }

    @Test
    @DisplayName("a delivered shipment reports the outcome, not a forecast")
    void deliveredShipmentReportsOutcome() {
        Shipment delivered = shipment(ShipmentStatus.DELIVERED, LocalDate.of(2026, 3, 8));
        delivered.setActualDeliveryDate(LocalDate.of(2026, 3, 10));

        EtaCalculator.Result result = EtaCalculator.calculate(delivered, List.of(), event(NOW), NOW);

        assertEquals(0, result.delayRiskScore());
        assertEquals(100, result.confidenceScore());
        assertEquals(EtaCalculator.SOURCE_COMPLETED, result.source());
        assertTrue(result.expectedDelayMinutes() > 0, "delivered two days late");
    }

    @Test
    @DisplayName("a cancelled shipment has no predicted arrival")
    void cancelledShipmentHasNoEta() {
        EtaCalculator.Result result = EtaCalculator.calculate(
                shipment(ShipmentStatus.CANCELLED, LocalDate.of(2026, 3, 12)), List.of(), null, NOW);

        assertNull(result.predictedDeliveryAt());
        assertEquals(0, result.delayRiskScore());
    }

    @Test
    @DisplayName("distance fills in for a leg with no duration at all")
    void derivesMinutesFromDistance() {
        DeliveryRoute bare = DeliveryRoute.builder()
                .id(1L).legNumber(1).status(RouteLegStatus.ACTIVE)
                .distanceKm(new BigDecimal("40.00"))
                .metricsSource("MANUAL")
                .build();

        EtaCalculator.Result result = EtaCalculator.calculate(
                shipment(ShipmentStatus.IN_TRANSIT, LocalDate.of(2026, 3, 12)),
                List.of(bare), event(NOW.minusMinutes(5)), NOW);

        // 40 km at the 40 km/h fallback speed is about an hour
        assertEquals(NOW.plusMinutes(60), result.predictedDeliveryAt());
        assertEquals(70, result.confidenceScore(), "estimated metrics are less trustworthy");
    }

    @Test
    @DisplayName("scores map onto the right risk buckets")
    void riskLevelBoundaries() {
        assertEquals(DelayRiskLevel.LOW, DelayRiskLevel.fromScore(0));
        assertEquals(DelayRiskLevel.LOW, DelayRiskLevel.fromScore(24));
        assertEquals(DelayRiskLevel.MEDIUM, DelayRiskLevel.fromScore(25));
        assertEquals(DelayRiskLevel.HIGH, DelayRiskLevel.fromScore(50));
        assertEquals(DelayRiskLevel.CRITICAL, DelayRiskLevel.fromScore(75));
        assertEquals(DelayRiskLevel.CRITICAL, DelayRiskLevel.fromScore(100));
    }

    @Test
    @DisplayName("the score never leaves 0-100 however many penalties stack")
    void scoreStaysInRange() {
        Shipment worst = shipment(ShipmentStatus.FAILED_DELIVERY, LocalDate.of(2026, 1, 1));
        List<DeliveryRoute> legs = List.of(
                leg(1, RouteLegStatus.ACTIVE, 10 * 24 * 60, 12 * 24 * 60, "SEVERE", "MANUAL"));

        EtaCalculator.Result result = EtaCalculator.calculate(worst, legs, null, NOW);

        assertTrue(result.delayRiskScore() <= 100 && result.delayRiskScore() >= 0);
        assertTrue(result.confidenceScore() >= 10 && result.confidenceScore() <= 95);
    }
}
