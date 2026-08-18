package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.CancelShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.AssignOperatorRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;
import com.shiptrack.shiptrack_pro.dto.ShipmentUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.StatusUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShipmentService {

    ShipmentResponse createShipment(ShipmentRequest request);

    Page<ShipmentResponse> getShipments(String status, Pageable pageable);

    ShipmentResponse getShipmentById(Long id);

    ShipmentResponse getShipmentByTrackingNumber(String trackingNumber);

    ShipmentResponse updateShipment(Long id, ShipmentUpdateRequest request);

    ShipmentResponse updateStatus(Long id, StatusUpdateRequest request);

    ShipmentResponse cancelShipment(Long id, CancelShipmentRequest request);

    ShipmentResponse assignOperator(Long id, AssignOperatorRequest request);
}
