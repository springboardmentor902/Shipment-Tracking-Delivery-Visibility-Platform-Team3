package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.LocationUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.TrackingEventRequest;
import com.shiptrack.shiptrack_pro.dto.TrackingEventResponse;
import com.shiptrack.shiptrack_pro.dto.TrackingResponse;

import java.util.List;

public interface TrackingService {

    List<TrackingEventResponse> getShipmentEvents(Long shipmentId);

    TrackingResponse getTracking(String trackingNumber);

    /** Add a checkpoint to the timeline by hand. Assigned operator or admin only. */
    TrackingEventResponse addEvent(TrackingEventRequest request);

    /** Store a live position ping and move the route leg's last known location. */
    TrackingEventResponse recordLocation(LocationUpdateRequest request);
}
