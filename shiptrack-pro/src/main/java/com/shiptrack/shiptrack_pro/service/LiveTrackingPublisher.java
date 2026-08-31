package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.TrackingEventResponse;
import com.shiptrack.shiptrack_pro.entity.Shipment;

/**
 * Pushes tracking changes to subscribed browsers.
 *
 * Broadcasting is best-effort: a broker problem must never fail the business
 * operation that produced the update.
 */
public interface LiveTrackingPublisher {

    /** Live driver ping. */
    void publishLocation(Shipment shipment, TrackingEventResponse event);

    /** Manually added checkpoint. */
    void publishCheckpoint(Shipment shipment, TrackingEventResponse event);

    /** Lifecycle change such as OUT_FOR_DELIVERY or DELIVERED. */
    void publishStatus(Shipment shipment, String notes, String actorName);
}
