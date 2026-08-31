package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.GeoPoint;
import com.shiptrack.shiptrack_pro.dto.LocationUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.TrackingEventRequest;
import com.shiptrack.shiptrack_pro.dto.TrackingEventResponse;
import com.shiptrack.shiptrack_pro.dto.TrackingResponse;
import com.shiptrack.shiptrack_pro.entity.DeliveryRoute;
import com.shiptrack.shiptrack_pro.entity.RouteLegStatus;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.DeliveryRouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.MapsService;
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import com.shiptrack.shiptrack_pro.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final DeliveryRouteRepository deliveryRouteRepository;
    private final ShipmentService shipmentService;
    private final CurrentUserService currentUserService;
    private final MapsService mapsService;

    /* ===================== history ===================== */

    @Override
    @Transactional(readOnly = true)
    public List<TrackingEventResponse> getShipmentEvents(Long shipmentId) {
        // throws 403/404 when the caller may not see this shipment
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

    /* ===================== manual checkpoint ===================== */

    @Override
    @Transactional
    public TrackingEventResponse addEvent(TrackingEventRequest request) {
        User actor = currentUserService.getCurrentUser();
        Shipment shipment = findShipment(request.getShipmentId());
        assertCanRecord(shipment, actor);
        assertShipmentIsOpen(shipment);

        BigDecimal latitude = request.getLatitude();
        BigDecimal longitude = request.getLongitude();

        // No coordinates typed? Try to resolve them from the location text so the
        // event still shows up on the map.
        if ((latitude == null || longitude == null) && mapsService.isEnabled()) {
            Optional<GeoPoint> geocoded = mapsService.geocode(request.getLocation());
            if (geocoded.isPresent()) {
                latitude = geocoded.get().latitude();
                longitude = geocoded.get().longitude();
            }
        }

        TrackingEvent event = trackingEventRepository.save(TrackingEvent.builder()
                .shipment(shipment)
                .status(shipment.getStatus())
                .location(request.getLocation())
                .latitude(latitude)
                .longitude(longitude)
                .notes(request.getNotes())
                .recordedBy(actor)
                .build());

        updateActiveLeg(shipment, actor, latitude, longitude);
        return toResponse(event);
    }

    /* ===================== live location ping ===================== */

    @Override
    @Transactional
    public TrackingEventResponse recordLocation(LocationUpdateRequest request) {
        User actor = currentUserService.getCurrentUser();
        Shipment shipment = findShipment(request.getShipmentId());
        assertCanRecord(shipment, actor);
        assertShipmentIsOpen(shipment);

        TrackingEvent event = trackingEventRepository.save(TrackingEvent.builder()
                .shipment(shipment)
                .status(shipment.getStatus())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .notes(request.getNotes())
                .recordedBy(actor)
                .build());

        updateActiveLeg(shipment, actor, request.getLatitude(), request.getLongitude());
        return toResponse(event);
    }

    /* ===================== helpers ===================== */

    /**
     * Only the operator responsible for this shipment (its assigned operator or
     * the driver of one of its legs) and administrators may write tracking data.
     */
    private void assertCanRecord(Shipment shipment, User actor) {
        Role role = Role.valueOf(actor.getRole());
        if (role == Role.ADMINISTRATOR) {
            return;
        }
        if (role != Role.LOGISTICS_OPERATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only LOGISTICS_OPERATOR and ADMINISTRATOR can record tracking updates. Your role: " + role);
        }

        boolean assigned = shipment.getAssignedOperator() != null
                && Objects.equals(shipment.getAssignedOperator().getId(), actor.getId());
        boolean drivesALeg = deliveryRouteRepository.findByShipmentIdOrderByLegNumberAsc(shipment.getId()).stream()
                .anyMatch(leg -> leg.getDriver() != null
                        && Objects.equals(leg.getDriver().getId(), actor.getId()));

        if (!assigned && !drivesALeg) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not assigned to this shipment");
        }
    }

    private void assertShipmentIsOpen(Shipment shipment) {
        if (shipment.getStatus() == ShipmentStatus.DELIVERED || shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot record tracking updates for a " + shipment.getStatus() + " shipment");
        }
    }

    /**
     * Keeps the route leg in sync with the latest ping: the leg the driver is on
     * (or the first unfinished leg) stores the last known position and becomes
     * ACTIVE.
     */
    private void updateActiveLeg(Shipment shipment, User actor, BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return;
        }

        List<DeliveryRoute> legs = deliveryRouteRepository.findByShipmentIdOrderByLegNumberAsc(shipment.getId());
        if (legs.isEmpty()) {
            return;
        }

        DeliveryRoute target = legs.stream()
                .filter(leg -> leg.getDriver() != null && Objects.equals(leg.getDriver().getId(), actor.getId()))
                .filter(leg -> leg.getStatus() != RouteLegStatus.COMPLETED)
                .findFirst()
                .orElseGet(() -> legs.stream()
                        .filter(leg -> leg.getStatus() != RouteLegStatus.COMPLETED
                                && leg.getStatus() != RouteLegStatus.SKIPPED)
                        .findFirst()
                        .orElse(null));

        if (target == null) {
            return;
        }

        target.setLastKnownLatitude(latitude);
        target.setLastKnownLongitude(longitude);
        target.setLastLocationAt(LocalDateTime.now());
        if (target.getStatus() == RouteLegStatus.PLANNED) {
            target.setStatus(RouteLegStatus.ACTIVE);
        }
        deliveryRouteRepository.save(target);
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
