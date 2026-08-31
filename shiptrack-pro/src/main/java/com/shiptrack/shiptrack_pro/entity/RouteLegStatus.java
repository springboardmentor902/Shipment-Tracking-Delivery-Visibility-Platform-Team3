package com.shiptrack.shiptrack_pro.entity;

/**
 * Lifecycle of a single route leg.
 *
 * A shipment is moved through one or more legs; each leg is planned first,
 * becomes ACTIVE while the assigned driver is on it, and ends COMPLETED
 * or SKIPPED when the plan changes.
 */
public enum RouteLegStatus {
    PLANNED,
    ACTIVE,
    COMPLETED,
    SKIPPED
}
