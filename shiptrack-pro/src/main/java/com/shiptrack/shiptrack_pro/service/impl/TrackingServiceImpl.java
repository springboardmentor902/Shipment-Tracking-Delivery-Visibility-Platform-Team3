package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.LocationUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.TrackingEventResponse;
import com.shiptrack.shiptrack_pro.dto.TrackingResponse;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import com.shiptrack.shiptrack_pro.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final ShipmentService shipmentService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<TrackingEventResponse> getShipmentEvents(Long shipmentId) {
        shipmentService.getShipmentById(shipmentId);
        Shipment shipment = findShipment(shipmentId);
        return trackingEventRepository.findByShipmentOrderByRecordedAtAsc(shipment).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingResponse getTracking(String trackingNumber) {
        return TrackingResponse.builder()
                .shipment(shipmentService.getShipmentByTrackingNumber(trackingNumber))
                .events(trackingEventRepository.findByShipmentOrderByRecordedAtAsc(findShipment(trackingNumber)).stream()
                        .map(this::toResponse)
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public TrackingEventResponse recordLocation(LocationUpdateRequest request) {
        User actor = currentUserService.getCurrentUser();
        Role role = Role.valueOf(actor.getRole());
        if (role != Role.LOGISTICS_OPERATOR && role != Role.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only LOGISTICS_OPERATOR and ADMINISTRATOR can update location");
        }

        Shipment shipment = findShipment(request.getShipmentId());
        if (shipment.getStatus() == ShipmentStatus.DELIVERED || shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot update location for a " + shipment.getStatus() + " shipment");
        }

        TrackingEvent event = trackingEventRepository.save(TrackingEvent.builder()
                .shipment(shipment)
                .status(shipment.getStatus())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .notes(request.getNotes())
                .recordedBy(actor)
                .build());
        return toResponse(event);
    }

    private Shipment findShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + shipmentId));
    }

    private Shipment findShipment(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shipment not found with tracking number: " + trackingNumber));
    }

    private TrackingEventResponse toResponse(TrackingEvent event) {
        return TrackingEventResponse.builder()
                .id(event.getId())
                .status(event.getStatus().name())
                .location(event.getLocation())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .notes(event.getNotes())
                .recordedByName(event.getRecordedBy() == null ? null : event.getRecordedBy().getFullName())
                .recordedAt(event.getRecordedAt())
                .build();
    }
}
