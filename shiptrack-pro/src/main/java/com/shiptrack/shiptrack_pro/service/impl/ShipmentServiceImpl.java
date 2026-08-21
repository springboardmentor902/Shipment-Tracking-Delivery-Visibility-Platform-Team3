package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    // =========================================================
    // CREATE SHIPMENT
    // =========================================================

    @Override
    public ShipmentResponse createShipment(
            ShipmentRequest request,
            String customerEmail) {

        // Find the logged-in customer using JWT email
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found: " + customerEmail
                ));

        Shipment shipment = Shipment.builder()
                .trackingNumber(generateTrackingNumber())
                .customer(customer)
                .senderName(request.getSenderName())
                .senderAddress(request.getSenderAddress())
                .receiverName(request.getReceiverName())
                .receiverAddress(request.getReceiverAddress())
                .receiverPhone(request.getReceiverPhone())
                .packageDescription(request.getPackageDescription())
                .weightKg(request.getWeightKg())
                .status("CREATED")
                .build();

        Shipment savedShipment = shipmentRepository.save(shipment);

        return mapToResponse(savedShipment);
    }


    // =========================================================
    // GET ALL SHIPMENTS OF LOGGED-IN CUSTOMER
    // =========================================================

    @Override
    public List<ShipmentResponse> getCustomerShipments(
            String customerEmail) {

        // Find logged-in customer
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found: " + customerEmail
                ));

        // Get only this customer's shipments
        return shipmentRepository.findByCustomer(customer)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET ONE SHIPMENT USING TRACKING NUMBER
    // =========================================================

    @Override
    public ShipmentResponse getCustomerShipmentByTrackingNumber(
            String trackingNumber,
            String customerEmail) {

        // Find logged-in customer
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found: " + customerEmail
                ));

        // Search tracking number belonging to this customer
        Shipment shipment = shipmentRepository
                .findByTrackingNumberAndCustomer(
                        trackingNumber,
                        customer
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found"
                ));

        return mapToResponse(shipment);
    }


    // =========================================================
    // GENERATE UNIQUE TRACKING NUMBER
    // =========================================================

    private String generateTrackingNumber() {

        String candidate;

        do {

            candidate = "STP-"
                    + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase();

        } while (
                shipmentRepository.existsByTrackingNumber(candidate)
        );

        return candidate;
    }


    // =========================================================
    // CONVERT ENTITY TO RESPONSE DTO
    // =========================================================

    private ShipmentResponse mapToResponse(
            Shipment shipment) {

        return ShipmentResponse.builder()
                .id(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .customerId(shipment.getCustomer().getId())
                .customerEmail(shipment.getCustomer().getEmail())
                .senderName(shipment.getSenderName())
                .senderAddress(shipment.getSenderAddress())
                .receiverName(shipment.getReceiverName())
                .receiverAddress(shipment.getReceiverAddress())
                .receiverPhone(shipment.getReceiverPhone())
                .packageDescription(shipment.getPackageDescription())
                .weightKg(shipment.getWeightKg())
                .status(shipment.getStatus())
                .createdAt(shipment.getCreatedAt())
                .build();
    }
}