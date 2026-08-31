package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.EtaResponse;
import com.shiptrack.shiptrack_pro.entity.Shipment;

import java.util.List;

public interface EtaService {

    /** Recalculates and stores the forecast for one shipment. */
    EtaResponse recalculate(Long shipmentId);

    /** Same, when the caller already holds the entity (avoids a reload). */
    EtaResponse recalculate(Shipment shipment);

    /** Best-effort refresh used by write paths; never throws. */
    void refreshQuietly(Long shipmentId);

    /** Stored forecast for a shipment the caller is allowed to see. */
    EtaResponse getForShipment(Long shipmentId);

    /**
     * Shipments at or above the given risk score that the caller may see,
     * highest risk first.
     */
    List<EtaResponse> getAtRisk(int minimumScore);

    /** Recalculates every shipment still in flight. Returns how many were updated. */
    int recalculateActive();
}
