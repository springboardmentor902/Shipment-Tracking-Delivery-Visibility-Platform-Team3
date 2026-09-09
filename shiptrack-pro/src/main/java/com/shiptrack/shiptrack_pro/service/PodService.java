package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.PodResponse;
import com.shiptrack.shiptrack_pro.dto.PodSubmitRequest;
import com.shiptrack.shiptrack_pro.dto.PodSummaryResponse;
import com.shiptrack.shiptrack_pro.dto.PodVerifyRequest;

import java.util.List;

public interface PodService {

    // Driver submits signature + photo for a shipment
    PodResponse submitProof(Long shipmentId, PodSubmitRequest request, String driverEmail);

    // Queue: all proofs currently pending verification
    List<PodSummaryResponse> getPendingQueue();

    // Full detail for one proof (signature + photo)
    PodResponse getProofByShipmentId(Long shipmentId);

    // Approve or reject
    PodResponse verifyProof(Long shipmentId, PodVerifyRequest request, String verifierEmail);
}
