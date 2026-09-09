package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.ETAPredictionRequest;
import com.shiptrack.shiptrack_pro.dto.ETAPredictionResponse;

public interface ETAPredictionService {

    ETAPredictionResponse predictETA(ETAPredictionRequest request);

    ETAPredictionResponse getETAByShipmentId(Long shipmentId);

    void recalculateETA(Long shipmentId);
}