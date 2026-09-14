package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.ETAPredictionRequest;
import com.shiptrack.shiptrack_pro.dto.ETAPredictionResponse;

import java.util.List;

public interface ETAPredictionService {

    ETAPredictionResponse predictETA(
            ETAPredictionRequest request
    );

    void recalculateETA(Long shipmentId);

    List<ETAPredictionResponse> getAtRiskPredictions(
            double minScore
    );

    ETAPredictionResponse getETAByShipmentId(
            Long shipmentId
    );
}