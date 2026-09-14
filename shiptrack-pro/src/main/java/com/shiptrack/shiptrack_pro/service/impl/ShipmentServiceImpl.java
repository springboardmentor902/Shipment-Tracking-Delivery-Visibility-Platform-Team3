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
import com.shiptrack.shiptrack_pro.service.RouteService;
import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final RouteService routeService;
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
                .senderPhone(request.getSenderPhone())
                .receiverEmail(request.getReceiverEmail())
                .senderAddress(request.getSenderAddress())
                .receiverName(request.getReceiverName())
                .receiverAddress(request.getReceiverAddress())
                .receiverPhone(request.getReceiverPhone())
                .packageDescription(request.getPackageDescription())
                .weightKg(request.getWeightKg())
                .status("CREATED")
                .build();

        Shipment savedShipment = shipmentRepository.save(shipment);
        RouteRequest routeRequest = new RouteRequest();

        routeRequest.setShipmentId(savedShipment.getId());
        routeRequest.setOrigin(savedShipment.getSenderAddress());
        routeRequest.setDestination(savedShipment.getReceiverAddress());
        routeRequest.setTrafficCondition("NORMAL");
        routeRequest.setDriverId(null);

        routeService.createRoute(routeRequest);

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
 // ADMIN - GET ALL SHIPMENTS
 // =========================================================
    @Override
    public ShipmentResponse updateShipmentStatus(String trackingNumber, String status) {

        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Shipment not found"));

        shipment.setStatus(status);

        Shipment savedShipment = shipmentRepository.save(shipment);

        return mapToResponse(savedShipment);
    }
 @Override
 public List<ShipmentResponse> getAllShipments() {

     return shipmentRepository.findAll()
             .stream()
             .map(this::mapToResponse)
             .toList();
 }


 // =========================================================
 // ADMIN - GET SHIPMENT BY TRACKING NUMBER
 // =========================================================

 @Override
 public ShipmentResponse getShipmentByTrackingNumber(
         String trackingNumber) {

     Shipment shipment = shipmentRepository
             .findByTrackingNumber(trackingNumber)
             .orElseThrow(() -> new ResponseStatusException(
                     HttpStatus.NOT_FOUND,
                     "Shipment not found"
             ));

     return mapToResponse(shipment);
 }
//=========================================================
//ASSIGN LOGISTICS OPERATOR
//=========================================================

@Override
public ShipmentResponse assignOperator(
      String trackingNumber,
      Long operatorId) {

  Shipment shipment = shipmentRepository
          .findByTrackingNumber(trackingNumber)
          .orElseThrow(() -> new ResponseStatusException(
                  HttpStatus.NOT_FOUND,
                  "Shipment not found"
          ));

  User operator = userRepository
          .findById(operatorId)
          .orElseThrow(() -> new ResponseStatusException(
                  HttpStatus.NOT_FOUND,
                  "Operator not found"
          ));

  if (!"LOGISTICS_OPERATOR".equals(operator.getRole())) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Selected user is not a logistics operator"
      );
  }

  shipment.setOperator(operator);

  Shipment savedShipment = shipmentRepository.save(shipment);

  return mapToResponse(savedShipment);
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

    private ShipmentResponse mapToResponse(Shipment shipment) {

        String createdByName = null;
        String createdByRole = null;

        if (shipment.getCustomer() != null) {
            createdByName = shipment.getCustomer().getFullName();
            createdByRole = shipment.getCustomer().getRole();
        }

        String assignedOperatorName = null;
        Long assignedOperatorId = null;

        if (shipment.getOperator() != null) {
            assignedOperatorName = shipment.getOperator().getFullName();
            assignedOperatorId = shipment.getOperator().getId();
        }

        return ShipmentResponse.builder()
                .id(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())

                .customerId(
                        shipment.getCustomer() != null
                                ? shipment.getCustomer().getId()
                                : null
                )

                .customerEmail(
                        shipment.getCustomer() != null
                                ? shipment.getCustomer().getEmail()
                                : null
                )

                .senderName(shipment.getSenderName())
                .senderAddress(shipment.getSenderAddress())

                .receiverName(shipment.getReceiverName())
                .receiverAddress(shipment.getReceiverAddress())
                .receiverPhone(shipment.getReceiverPhone())

                .packageDescription(shipment.getPackageDescription())
                .weightKg(shipment.getWeightKg())

                .status(shipment.getStatus())

                .createdByName(createdByName)
                .createdByRole(createdByRole)

                .assignedOperatorName(assignedOperatorName)
                .assignedOperatorId(assignedOperatorId)

                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())

                .build();
    }
}