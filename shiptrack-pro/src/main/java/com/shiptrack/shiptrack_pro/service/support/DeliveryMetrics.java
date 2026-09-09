package com.shiptrack.shiptrack_pro.service.support;

import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central place for the delivery/route math so Analytics and Reports
 * compute delay, on-time, and status-breakdown figures the exact same way
 * instead of each re-implementing it.
 */
public final class DeliveryMetrics {

    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_REJECTED = "DELIVERY_REJECTED";

    private DeliveryMetrics() {}

    public static boolean isDelivered(Shipment shipment) {
        return STATUS_DELIVERED.equals(shipment.getStatus());
    }

    public static boolean isRejected(Shipment shipment) {
        return STATUS_REJECTED.equals(shipment.getStatus());
    }

    public static boolean isActive(Shipment shipment) {
        return !isDelivered(shipment) && !isRejected(shipment);
    }

    public static Map<String, Long> statusBreakdown(List<Shipment> shipments) {
        return shipments.stream()
                .collect(Collectors.groupingBy(Shipment::getStatus, Collectors.counting()));
    }

    // Actual delivery time in hours, measured from creation to the moment the
    // shipment last changed (a proxy for "delivered at" since we don't store
    // a dedicated deliveredAt field on Shipment itself).
    public static Double deliveryDurationHours(Shipment shipment) {
        if (shipment.getCreatedAt() == null || shipment.getUpdatedAt() == null) return null;
        long minutes = Duration.between(shipment.getCreatedAt(), shipment.getUpdatedAt()).toMinutes();
        return minutes / 60.0;
    }

    // On-time = actual transit time did not exceed the estimated transit time.
    // Returns null (unknown) when the route hasn't recorded both figures yet.
    public static Boolean isOnTime(Route route) {
        if (route == null || route.getEstimatedTimeMinutes() == null || route.getActualTimeMinutes() == null) {
            return null;
        }
        return route.getActualTimeMinutes() <= route.getEstimatedTimeMinutes();
    }

    // Delay in minutes (actual - estimated). Negative/zero means on time or early.
    // Returns null when the route hasn't recorded both figures yet.
    public static Integer delayMinutes(Route route) {
        if (route == null || route.getEstimatedTimeMinutes() == null || route.getActualTimeMinutes() == null) {
            return null;
        }
        return route.getActualTimeMinutes() - route.getEstimatedTimeMinutes();
    }

    public static boolean isRecentlyCreated(Shipment shipment, int days) {
        if (shipment.getCreatedAt() == null) return false;
        return shipment.getCreatedAt().isAfter(LocalDateTime.now().minusDays(days));
    }
}
