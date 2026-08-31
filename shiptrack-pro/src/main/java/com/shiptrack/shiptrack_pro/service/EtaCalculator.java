package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.entity.DelayRiskLevel;
import com.shiptrack.shiptrack_pro.entity.DeliveryRoute;
import com.shiptrack.shiptrack_pro.entity.RouteLegStatus;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import com.shiptrack.shiptrack_pro.util.GeoUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Works out when a shipment will actually arrive and how likely it is to miss
 * its promise.
 *
 * Deliberately a pure class with no Spring or database dependencies: everything
 * it needs is passed in, so the arithmetic can be tested without a context and
 * the same call is cheap enough to run on every location ping.
 *
 * The forecast prefers real route metrics (Milestone 2 wrote traffic-aware
 * durations onto each leg). When no route has been planned yet it falls back to
 * a per-status guess, and says so through a lower confidence score.
 */
public final class EtaCalculator {

    public static final String SOURCE_ROUTE_METRICS = "ROUTE_METRICS";
    public static final String SOURCE_STATUS_HEURISTIC = "STATUS_HEURISTIC";
    public static final String SOURCE_COMPLETED = "COMPLETED";

    /** Rough remaining transit time per status when no route legs exist, in minutes. */
    private static final int MINUTES_CREATED = 3 * 24 * 60;
    private static final int MINUTES_PICKED_UP = 2 * 24 * 60;
    private static final int MINUTES_IN_TRANSIT = 24 * 60;
    private static final int MINUTES_OUT_FOR_DELIVERY = 4 * 60;
    private static final int MINUTES_FAILED_DELIVERY = 2 * 24 * 60;

    /** A tracked shipment that has not reported in this long looks neglected. */
    private static final long STALE_HOURS = 6;
    private static final long VERY_STALE_HOURS = 24;

    private EtaCalculator() {
    }

    /**
     * @param shipment  the shipment being forecast
     * @param legs      its route legs, any order
     * @param lastEvent the most recent tracking event, or null if none
     * @param now       the reference time (injected so tests are deterministic)
     */
    public static Result calculate(Shipment shipment, List<DeliveryRoute> legs, TrackingEvent lastEvent,
                                  LocalDateTime now) {
        List<String> factors = new ArrayList<>();
        LocalDate promised = shipment.getEstimatedDeliveryDate();

        // A finished shipment has no future to predict; report the outcome instead.
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            LocalDate actual = shipment.getActualDeliveryDate();
            LocalDateTime deliveredAt = actual != null ? actual.atTime(LocalTime.NOON)
                    : (lastEvent != null ? lastEvent.getRecordedAt() : now);
            int delay = promised != null && actual != null
                    ? (int) Duration.between(endOfDay(promised), endOfDay(actual)).toMinutes()
                    : 0;
            factors.add(delay > 0
                    ? "Delivered " + humanise(delay) + " after the promised date."
                    : "Delivered on or before the promised date.");
            return new Result(deliveredAt, promised, delay, 0, DelayRiskLevel.LOW, 100,
                    factors, SOURCE_COMPLETED);
        }

        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            factors.add("Shipment was cancelled, so no delivery is expected.");
            return new Result(null, promised, null, 0, DelayRiskLevel.LOW, 100,
                    factors, SOURCE_COMPLETED);
        }

        List<DeliveryRoute> remainingLegs = legs.stream()
                .filter(leg -> leg.getStatus() != RouteLegStatus.COMPLETED
                        && leg.getStatus() != RouteLegStatus.SKIPPED)
                .toList();

        int remainingMinutes;
        String source;
        int confidence;

        if (remainingLegs.isEmpty() && !legs.isEmpty()) {
            // every leg is closed but the shipment is not marked delivered yet
            remainingMinutes = MINUTES_OUT_FOR_DELIVERY / 4;
            source = SOURCE_ROUTE_METRICS;
            confidence = 70;
            factors.add("All route legs are closed; awaiting the delivery confirmation.");
        } else if (remainingLegs.isEmpty()) {
            remainingMinutes = heuristicMinutes(shipment.getStatus());
            source = SOURCE_STATUS_HEURISTIC;
            confidence = 45;
            factors.add("No route planned yet, so the estimate comes from the current status only.");
        } else {
            remainingMinutes = 0;
            boolean anyLiveMetrics = false;
            boolean anyEstimatedMetrics = false;
            for (DeliveryRoute leg : remainingLegs) {
                remainingMinutes += legMinutes(leg);
                if ("LIVE_MAPS".equals(leg.getMetricsSource())) {
                    anyLiveMetrics = true;
                } else {
                    anyEstimatedMetrics = true;
                }
            }
            source = SOURCE_ROUTE_METRICS;
            confidence = anyLiveMetrics && !anyEstimatedMetrics ? 90 : 70;
            factors.add(remainingLegs.size() == 1
                    ? "One route leg left, " + humanise(remainingMinutes) + " of travel time."
                    : remainingLegs.size() + " route legs left, " + humanise(remainingMinutes)
                            + " of travel time in total.");
            if (anyEstimatedMetrics) {
                factors.add("Some legs use estimated rather than live traffic data.");
            }
        }

        // Traffic on the leg being driven right now stretches the remaining time.
        String worstTraffic = worstTraffic(remainingLegs);
        double trafficMultiplier = switch (worstTraffic == null ? "" : worstTraffic) {
            case "SEVERE" -> 1.30;
            case "HEAVY" -> 1.15;
            default -> 1.0;
        };
        if (trafficMultiplier > 1.0) {
            remainingMinutes = (int) Math.round(remainingMinutes * trafficMultiplier);
            factors.add(worstTraffic.charAt(0) + worstTraffic.substring(1).toLowerCase()
                    + " traffic on the active leg adds to the travel time.");
        }

        // Count from the last sighting when it is in the future of nothing else,
        // otherwise from now: a ping five hours old does not push the ETA back.
        LocalDateTime from = now;
        if (lastEvent != null && lastEvent.getRecordedAt() != null
                && lastEvent.getRecordedAt().isAfter(now)) {
            from = lastEvent.getRecordedAt();
        }
        LocalDateTime predicted = from.plusMinutes(remainingMinutes);

        Integer delayMinutes = null;
        int risk = 0;

        if (promised != null) {
            delayMinutes = (int) Duration.between(endOfDay(promised), predicted).toMinutes();
            if (delayMinutes > 0) {
                // ten points per hour late, up to sixty
                risk += Math.min(60, (int) Math.round(delayMinutes / 60.0 * 10));
                factors.add("Running about " + humanise(delayMinutes) + " past the promised date.");
            } else {
                factors.add("On track to arrive within the promised window.");
            }
            if (endOfDay(promised).isBefore(now)) {
                risk += 30;
                factors.add("The promised delivery date has already passed.");
            }
        } else {
            confidence -= 10;
            factors.add("No promised delivery date on the shipment to compare against.");
        }

        // Silence is itself a warning sign once a shipment is moving.
        boolean moving = shipment.getStatus() == ShipmentStatus.IN_TRANSIT
                || shipment.getStatus() == ShipmentStatus.OUT_FOR_DELIVERY;
        if (moving) {
            LocalDateTime lastSeen = lastEvent != null ? lastEvent.getRecordedAt() : null;
            long hoursQuiet = lastSeen == null ? Long.MAX_VALUE : Duration.between(lastSeen, now).toHours();
            if (hoursQuiet >= VERY_STALE_HOURS) {
                risk += 25;
                confidence -= 20;
                factors.add(lastSeen == null
                        ? "No tracking update has ever been recorded for this shipment."
                        : "No tracking update for over a day.");
            } else if (hoursQuiet >= STALE_HOURS) {
                risk += 15;
                confidence -= 10;
                factors.add("No tracking update for more than " + STALE_HOURS + " hours.");
            }
        }

        if (shipment.getStatus() == ShipmentStatus.FAILED_DELIVERY) {
            risk += 35;
            factors.add("A delivery attempt already failed and needs rescheduling.");
        }

        if ("SEVERE".equals(worstTraffic)) {
            risk += 20;
        } else if ("HEAVY".equals(worstTraffic)) {
            risk += 10;
        }

        if (legs.isEmpty() && shipment.getStatus() != ShipmentStatus.CREATED) {
            risk += 10;
            factors.add("The shipment has moved but no route has been planned.");
        }

        risk = clamp(risk, 0, 100);
        confidence = clamp(confidence, 10, 95);

        return new Result(predicted, promised, delayMinutes, risk, DelayRiskLevel.fromScore(risk),
                confidence, factors, source);
    }

    /** Travel time for one leg, preferring traffic-aware data, then distance. */
    private static int legMinutes(DeliveryRoute leg) {
        if (leg.getDurationInTrafficMinutes() != null && leg.getDurationInTrafficMinutes() > 0) {
            return leg.getDurationInTrafficMinutes();
        }
        if (leg.getExpectedDurationMinutes() != null && leg.getExpectedDurationMinutes() > 0) {
            return leg.getExpectedDurationMinutes();
        }
        Integer fromDistance = GeoUtils.estimatedDurationMinutes(leg.getDistanceKm());
        return fromDistance != null && fromDistance > 0 ? fromDistance : MINUTES_IN_TRANSIT;
    }

    private static String worstTraffic(List<DeliveryRoute> legs) {
        String worst = null;
        int worstRank = 0;
        for (DeliveryRoute leg : legs) {
            int rank = switch (leg.getTrafficCondition() == null ? "" : leg.getTrafficCondition()) {
                case "SEVERE" -> 4;
                case "HEAVY" -> 3;
                case "MODERATE" -> 2;
                case "LIGHT" -> 1;
                default -> 0;
            };
            if (rank > worstRank) {
                worstRank = rank;
                worst = leg.getTrafficCondition();
            }
        }
        return worst;
    }

    private static int heuristicMinutes(ShipmentStatus status) {
        return switch (status) {
            case CREATED -> MINUTES_CREATED;
            case PICKED_UP -> MINUTES_PICKED_UP;
            case IN_TRANSIT -> MINUTES_IN_TRANSIT;
            case OUT_FOR_DELIVERY -> MINUTES_OUT_FOR_DELIVERY;
            case FAILED_DELIVERY -> MINUTES_FAILED_DELIVERY;
            default -> MINUTES_IN_TRANSIT;
        };
    }

    private static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.of(23, 59));
    }

    private static String humanise(int minutes) {
        int absolute = Math.abs(minutes);
        if (absolute < 60) return absolute + " min";
        if (absolute < 24 * 60) {
            long hours = Math.round(absolute / 60.0);
            return hours + (hours == 1 ? " hour" : " hours");
        }
        long days = Math.round(absolute / (24 * 60.0));
        return days + (days == 1 ? " day" : " days");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Immutable outcome of one forecast. */
    public record Result(LocalDateTime predictedDeliveryAt,
                         LocalDate promisedDeliveryDate,
                         Integer expectedDelayMinutes,
                         int delayRiskScore,
                         DelayRiskLevel riskLevel,
                         int confidenceScore,
                         List<String> factors,
                         String source) {
    }
}
