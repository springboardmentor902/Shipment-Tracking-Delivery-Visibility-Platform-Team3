package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.ActiveDeliveryResponse;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {

    private static final Set<ShipmentStatus> ACTIVE_STATUSES = Set.of(
            ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY);

    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<ActiveDeliveryResponse> getActiveDeliveries() {
        User actor = currentUserService.getCurrentUser();
        Role role = Role.valueOf(actor.getRole());
        if (role != Role.LOGISTICS_OPERATOR && role != Role.SUPPORT_AGENT && role != Role.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only LOGISTICS_OPERATOR, SUPPORT_AGENT and ADMINISTRATOR can view active deliveries");
        }

        return shipmentRepository.findByStatusIn(ACTIVE_STATUSES).stream()
                .filter(shipment -> role != Role.LOGISTICS_OPERATOR
                        || (shipment.getAssignedOperator() != null
                        && Objects.equals(shipment.getAssignedOperator().getId(), actor.getId())))
                .map(this::toResponse)
                .toList();
    }

    private ActiveDeliveryResponse toResponse(Shipment shipment) {
        Optional<TrackingEvent> lastEvent = trackingEventRepository
                .findFirstByShipmentOrderByRecordedAtDesc(shipment);
        TrackingEvent event = lastEvent.orElse(null);

        return ActiveDeliveryResponse.builder()
                .trackingNumber(shipment.getTrackingNumber())
                .shipmentId(shipment.getId())
                .status(shipment.getStatus().name())
                .priority(shipment.getPriority().name())
                .receiverName(shipment.getReceiverName())
                .deliveryAddress(shipment.getDeliveryAddress())
                .assignedOperatorName(shipment.getAssignedOperator() == null
                        ? null : shipment.getAssignedOperator().getFullName())
                .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                .lastLocation(event == null ? null : event.getLocation())
                .lastLatitude(event == null ? null : event.getLatitude())
                .lastLongitude(event == null ? null : event.getLongitude())
                .lastUpdatedAt(event == null ? null : event.getRecordedAt())
                .delayed(shipment.getEstimatedDeliveryDate() != null
                        && shipment.getEstimatedDeliveryDate().isBefore(LocalDate.now()))
                .build();
    }
}
