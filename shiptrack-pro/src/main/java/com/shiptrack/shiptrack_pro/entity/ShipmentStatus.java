package com.shiptrack.shiptrack_pro.entity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Closed set of shipment lifecycle states.
 *
 * A shipment can only move along the transitions declared here. Any other
 * jump (for example CREATED -> DELIVERED) is rejected by the service layer.
 */
public enum ShipmentStatus {

    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED_DELIVERY,
    CANCELLED;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<ShipmentStatus, Set<ShipmentStatus>> map = new EnumMap<>(ShipmentStatus.class);

        map.put(CREATED, EnumSet.of(PICKED_UP, CANCELLED));
        map.put(PICKED_UP, EnumSet.of(IN_TRANSIT, FAILED_DELIVERY, CANCELLED));
        map.put(IN_TRANSIT, EnumSet.of(OUT_FOR_DELIVERY, FAILED_DELIVERY, CANCELLED));
        map.put(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED, FAILED_DELIVERY));
        map.put(FAILED_DELIVERY, EnumSet.of(OUT_FOR_DELIVERY, CANCELLED));

        // Terminal states — nothing follows them.
        map.put(DELIVERED, EnumSet.noneOf(ShipmentStatus.class));
        map.put(CANCELLED, EnumSet.noneOf(ShipmentStatus.class));

        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    /** True when this status is allowed to move directly to {@code target}. */
    public boolean canTransitionTo(ShipmentStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    /** The states reachable from this one. Empty for terminal states. */
    public Set<ShipmentStatus> nextStates() {
        return ALLOWED_TRANSITIONS.get(this);
    }

    /** True when no further transition is possible. */
    public boolean isTerminal() {
        return ALLOWED_TRANSITIONS.get(this).isEmpty();
    }
}