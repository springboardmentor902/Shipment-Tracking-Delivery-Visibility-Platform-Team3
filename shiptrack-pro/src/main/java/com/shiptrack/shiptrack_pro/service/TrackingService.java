package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.LocationUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.TrackingEventResponse;
import com.shiptrack.shiptrack_pro.dto.TrackingResponse;

import java.util.List;

public interface TrackingService {

    List<TrackingEventResponse> getShipmentEvents(Long shipmentId);

    TrackingResponse getTracking(String trackingNumber);

    TrackingEventResponse recordLocation(LocationUpdateRequest request);
}
