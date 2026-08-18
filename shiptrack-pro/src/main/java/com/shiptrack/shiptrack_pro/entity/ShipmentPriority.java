package com.shiptrack.shiptrack_pro.entity;

/**
 * Closed set of delivery priorities, chosen by the creator at shipment creation.
 * Drives the estimated delivery date calculation until the ETA Prediction
 * module replaces it with Maps-based estimates.
 */
public enum ShipmentPriority {

    STANDARD(5),
    EXPRESS(2);

    private final int defaultTransitDays;

    ShipmentPriority(int defaultTransitDays) {
        this.defaultTransitDays = defaultTransitDays;
    }

    public int getDefaultTransitDays() {
        return defaultTransitDays;
    }
}