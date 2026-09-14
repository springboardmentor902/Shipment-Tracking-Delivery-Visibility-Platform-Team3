package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;

import java.util.List;

public interface ShipmentService {

    ShipmentResponse createShipment(
            ShipmentRequest request,
            String customerEmail
    );
    ShipmentResponse updateShipmentStatus(String trackingNumber, String status);
    List<ShipmentResponse> getCustomerShipments(
            String customerEmail
    );

    ShipmentResponse getCustomerShipmentByTrackingNumber(
            String trackingNumber,
            String customerEmail
    );

    // Admin shipment access
    List<ShipmentResponse> getAllShipments();

    ShipmentResponse getShipmentByTrackingNumber(
            String trackingNumber
    );
    ShipmentResponse assignOperator(
            String trackingNumber,
            Long operatorId
    );
    
}
