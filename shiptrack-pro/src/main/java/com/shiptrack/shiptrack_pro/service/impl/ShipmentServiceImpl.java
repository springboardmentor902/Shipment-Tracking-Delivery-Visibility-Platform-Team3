package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.*;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentPackage;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.ShipmentAccessPolicy;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.EtaService;
import com.shiptrack.shiptrack_pro.service.LiveTrackingPublisher;
import com.shiptrack.shiptrack_pro.service.NotificationService;
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private static final String TRACKING_PREFIX = "STP";
    private static final String TRACKING_ALPHABET = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // no I/O to avoid confusion
    private static final int TRACKING_BODY_LENGTH = 10;
    private static final int TRACKING_MAX_ATTEMPTS = 10;

    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ShipmentAccessPolicy accessPolicy;
    private final LiveTrackingPublisher liveTrackingPublisher;
    private final EtaService etaService;
    private final NotificationService notificationService;
    private final SecureRandom random = new SecureRandom();

    /* ===================== CREATE ===================== */

    @Override
    @Transactional
    public ShipmentResponse createShipment(ShipmentRequest request) {
        User actor = currentUserService.getCurrentUser();
        Role actorRole = Role.valueOf(actor.getRole());

        // Customers, business clients and logistics operators book shipments.
        // Support agents and administrators manage, they do not book (MM-20).
        if (actorRole != Role.CUSTOMER
                && actorRole != Role.BUSINESS_CLIENT
                && actorRole != Role.LOGISTICS_OPERATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only CUSTOMER, BUSINESS_CLIENT and LOGISTICS_OPERATOR can create shipments. Your role: "
                            + actorRole);
        }

        ShipmentPriority priority = parsePriority(request.getPriority(), ShipmentPriority.STANDARD);

        Shipment shipment = Shipment.builder()
                .trackingNumber(generateTrackingNumber())
                .createdBy(actor)                                  // taken from the token, never the body
                .businessId(actorRole == Role.BUSINESS_CLIENT ? actor.getId() : null)
                .assignedOperator(actorRole == Role.LOGISTICS_OPERATOR ? actor : null)
                .senderName(request.getSenderName())
                .senderPhone(request.getSenderPhone())
                .senderAddress(request.getSenderAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .receiverEmail(request.getReceiverEmail())
                .receiverAddress(request.getReceiverAddress())
                .pickupAddress(request.getPickupAddress())
                .deliveryAddress(request.getDeliveryAddress())
                .status(ShipmentStatus.CREATED)                    // every shipment starts here
                .priority(priority)
                .estimatedDeliveryDate(LocalDate.now().plusDays(priority.getDefaultTransitDays()))
                .build();

        int packageNo = 1;
        for (PackageRequest packageRequest : request.getPackages()) {
            shipment.addPackage(toPackageEntity(packageRequest, packageNo++));
        }

        Shipment savedShipment = shipmentRepository.save(shipment);
        logEvent(savedShipment, ShipmentStatus.CREATED, savedShipment.getPickupAddress(), null, actor);
        return toResponse(savedShipment);
    }

    /* ===================== READ ===================== */

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getShipments(String status, Pageable pageable) {
        User actor = currentUserService.getCurrentUser();
        Role actorRole = Role.valueOf(actor.getRole());
        ShipmentStatus filter = status == null || status.isBlank() ? null : parseStatus(status);

        Page<Shipment> page = switch (actorRole) {
            // Full visibility across the platform.
            case ADMINISTRATOR, SUPPORT_AGENT -> filter == null
                    ? shipmentRepository.findAll(pageable)
                    : shipmentRepository.findByStatus(filter, pageable);

            // Strictly the work assigned to this operator, nothing else.
            case LOGISTICS_OPERATOR -> filter == null
                    ? shipmentRepository.findByAssignedOperator(actor, pageable)
                    : shipmentRepository.findByAssignedOperatorAndStatus(actor, filter, pageable);

            // Shipments the customer booked plus shipments addressed to them.
            case CUSTOMER -> filter == null
                    ? shipmentRepository.findVisibleToCustomer(actor, actor.getEmail(), pageable)
                    : shipmentRepository.findVisibleToCustomerByStatus(actor, actor.getEmail(), filter, pageable);

            // Everything booked under this business account, including linked customers.
            case BUSINESS_CLIENT -> filter == null
                    ? shipmentRepository.findVisibleToBusiness(actor, actor.getId(), pageable)
                    : shipmentRepository.findVisibleToBusinessByStatus(actor, actor.getId(), filter, pageable);
        };

        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long id) {
        Shipment shipment = findOrThrow(id);
        assertCanView(shipment);
        return toResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shipment not found with tracking number: " + trackingNumber));

        // public tracking - check access only if someone is logged in
        if (currentUserService.isLoggedIn()) {
            assertCanView(shipment);
        }
        return toResponse(shipment);
    }

    /* ===================== UPDATE ===================== */

    @Override
    @Transactional
    public ShipmentResponse updateShipment(Long id, ShipmentUpdateRequest request) {
        Shipment shipment = findOrThrow(id);
        User actor = currentUserService.getCurrentUser();
        Role actorRole = Role.valueOf(actor.getRole());

        assertCanModify(shipment, actor, actorRole);

        if (shipment.getStatus().isTerminal()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot edit a shipment in terminal status " + shipment.getStatus());
        }

        if (request.getSenderName() != null)      shipment.setSenderName(request.getSenderName());
        if (request.getSenderPhone() != null)     shipment.setSenderPhone(request.getSenderPhone());
        if (request.getSenderAddress() != null)   shipment.setSenderAddress(request.getSenderAddress());
        if (request.getReceiverName() != null)    shipment.setReceiverName(request.getReceiverName());
        if (request.getReceiverPhone() != null)   shipment.setReceiverPhone(request.getReceiverPhone());
        if (request.getReceiverEmail() != null)   shipment.setReceiverEmail(request.getReceiverEmail());
        if (request.getReceiverAddress() != null) shipment.setReceiverAddress(request.getReceiverAddress());
        if (request.getPickupAddress() != null)   shipment.setPickupAddress(request.getPickupAddress());
        if (request.getDeliveryAddress() != null) shipment.setDeliveryAddress(request.getDeliveryAddress());

        if (request.getPriority() != null) {
            ShipmentPriority priority = parsePriority(request.getPriority(), null);
            shipment.setPriority(priority);
            if (shipment.getStatus() == ShipmentStatus.CREATED) {
                shipment.setEstimatedDeliveryDate(LocalDate.now().plusDays(priority.getDefaultTransitDays()));
            }
        }

        if (request.getAssignedOperatorId() != null) {
            if (actorRole != Role.ADMINISTRATOR && actorRole != Role.LOGISTICS_OPERATOR) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only ADMINISTRATOR or LOGISTICS_OPERATOR can assign an operator");
            }
            shipment.setAssignedOperator(resolveOperator(request.getAssignedOperatorId()));
        }

        if (request.getPackages() != null) {
            if (request.getPackages().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A shipment must contain at least one package");
            }
            shipment.clearPackages();
            int packageNo = 1;
            for (PackageRequest packageRequest : request.getPackages()) {
                shipment.addPackage(toPackageEntity(packageRequest, packageNo++));
            }
        }

        return toResponse(shipmentRepository.save(shipment));
    }

    /* ===================== STATUS TRANSITION ===================== */

    @Override
    @Transactional
    public ShipmentResponse updateStatus(Long id, StatusUpdateRequest request) {
        Shipment shipment = findOrThrow(id);
        User actor = currentUserService.getCurrentUser();
        Role actorRole = Role.valueOf(actor.getRole());

        // Moving a shipment through its lifecycle is operational work.
        if (actorRole != Role.LOGISTICS_OPERATOR && actorRole != Role.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only LOGISTICS_OPERATOR and ADMINISTRATOR can change shipment status. Your role: " + actorRole);
        }

        // An operator may only advance shipments they are responsible for.
        if (actorRole == Role.LOGISTICS_OPERATOR && !isTiedTo(shipment, actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not the assigned operator for this shipment");
        }

        ShipmentStatus target = parseStatus(request.getStatus());
        ShipmentStatus current = shipment.getStatus();

        if (target == ShipmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use DELETE /api/shipments/{id} to cancel a shipment");
        }

        if (target == current) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Shipment is already in status " + current);
        }

        if (!current.canTransitionTo(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Illegal status transition " + current + " -> " + target
                            + ". Allowed from " + current + ": " + current.nextStates());
        }

        shipment.setStatus(target);

        if (target == ShipmentStatus.DELIVERED) {
            shipment.setActualDeliveryDate(LocalDate.now());
        }

        Shipment savedShipment = shipmentRepository.save(shipment);
        logEvent(savedShipment, target, null, request.getNotes(), actor);
        liveTrackingPublisher.publishStatus(savedShipment, request.getNotes(), actor.getFullName());
        // the remaining journey just changed, so the forecast must too
        etaService.refreshQuietly(savedShipment.getId());
        notificationService.notifyStatusChange(savedShipment, request.getNotes(), actor.getFullName());
        return toResponse(savedShipment);
    }

    @Override
    @Transactional
    public ShipmentResponse assignOperator(Long id, AssignOperatorRequest request) {
        Shipment shipment = findOrThrow(id);
        User actor = currentUserService.getCurrentUser();
        Role actorRole = Role.valueOf(actor.getRole());
        if (actorRole != Role.ADMINISTRATOR && actorRole != Role.LOGISTICS_OPERATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only ADMINISTRATOR or LOGISTICS_OPERATOR can assign an operator");
        }

        User operator = resolveOperator(request.getOperatorId());
        shipment.setAssignedOperator(operator);
        Shipment savedShipment = shipmentRepository.save(shipment);
        logEvent(savedShipment, savedShipment.getStatus(), null, "assigned to " + operator.getFullName(), actor);
        return toResponse(savedShipment);
    }

    /* ===================== CANCEL ===================== */

    @Override
    @Transactional
    public ShipmentResponse cancelShipment(Long id, CancelShipmentRequest request) {
        Shipment shipment = findOrThrow(id);
        User actor = currentUserService.getCurrentUser();
        Role actorRole = Role.valueOf(actor.getRole());

        boolean isCreator = Objects.equals(shipment.getCreatedBy().getId(), actor.getId());
        if (actorRole != Role.ADMINISTRATOR && !isCreator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the creator of the shipment or an ADMINISTRATOR can cancel it");
        }

        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shipment is already cancelled");
        }

        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.CANCELLED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A shipment in status " + shipment.getStatus() + " can no longer be cancelled");
        }

        // Soft cancel: the row is retained for audit and analytics, never deleted.
        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipment.setCancelledAt(LocalDateTime.now());
        shipment.setCancellationReason(request.getReason());

        Shipment savedShipment = shipmentRepository.save(shipment);
        logEvent(savedShipment, ShipmentStatus.CANCELLED, null, request.getReason(), actor);
        liveTrackingPublisher.publishStatus(savedShipment, request.getReason(), actor.getFullName());
        etaService.refreshQuietly(savedShipment.getId());
        notificationService.notifyStatusChange(savedShipment, request.getReason(), actor.getFullName());
        return toResponse(savedShipment);
    }

    /* ===================== authorization helpers ===================== */

    private Shipment findOrThrow(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + id));
    }

    private boolean isCreator(Shipment shipment, User user) {
        return Objects.equals(shipment.getCreatedBy().getId(), user.getId());
    }

    private boolean isAssignedOperator(Shipment shipment, User user) {
        return shipment.getAssignedOperator() != null
                && Objects.equals(shipment.getAssignedOperator().getId(), user.getId());
    }

    /** Business clients own every shipment booked under their business id. */
    private boolean belongsToBusiness(Shipment shipment, User user) {
        return shipment.getBusinessId() != null
                && Objects.equals(shipment.getBusinessId(), user.getId());
    }

    private boolean isTiedTo(Shipment shipment, User user) {
        boolean isCreator = Objects.equals(shipment.getCreatedBy().getId(), user.getId());
        boolean isOperator = shipment.getAssignedOperator() != null
                && Objects.equals(shipment.getAssignedOperator().getId(), user.getId());
        return isCreator || isOperator;
    }

    // customer is linked by email only
    private boolean isReceiver(Shipment shipment, User user) {
        return shipment.getReceiverEmail() != null
                && shipment.getReceiverEmail().equalsIgnoreCase(user.getEmail());
    }

    private void assertCanView(Shipment shipment) {
        // same rules the live tracking socket applies to a SUBSCRIBE
        if (!accessPolicy.canView(shipment, currentUserService.getCurrentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have access to this shipment");
        }
    }

    private void assertCanModify(Shipment shipment, User actor, Role actorRole) {
        boolean allowed = switch (actorRole) {
            case ADMINISTRATOR -> true;
            case LOGISTICS_OPERATOR -> isAssignedOperator(shipment, actor);
            case BUSINESS_CLIENT -> isTiedTo(shipment, actor) || belongsToBusiness(shipment, actor);
            // a customer may correct the shipment they booked themselves
            case CUSTOMER -> isCreator(shipment, actor);
            case SUPPORT_AGENT -> false;
        };

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your role (" + actorRole + ") cannot modify this shipment");
        }
    }

    private User resolveOperator(Long operatorId) {
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + operatorId));

        if (!Role.LOGISTICS_OPERATOR.name().equals(operator.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User " + operatorId + " is not a LOGISTICS_OPERATOR");
        }
        return operator;
    }

    private void logEvent(Shipment shipment, ShipmentStatus status, String location, String notes, User actor) {
        trackingEventRepository.save(TrackingEvent.builder()
                .shipment(shipment)
                .status(status)
                .location(location)
                .notes(notes)
                .recordedBy(actor)
                .build());
    }

    /* ===================== parsing helpers ===================== */

    private ShipmentStatus parseStatus(String raw) {
        try {
            return ShipmentStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status: " + raw + ". Must be one of: "
                            + Arrays.toString(ShipmentStatus.values()));
        }
    }

    private ShipmentPriority parsePriority(String raw, ShipmentPriority fallback) {
        if (raw == null || raw.isBlank()) {
            if (fallback != null) {
                return fallback;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Priority cannot be blank");
        }
        try {
            return ShipmentPriority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid priority: " + raw + ". Must be one of: "
                            + Arrays.toString(ShipmentPriority.values()));
        }
    }

    private String generateTrackingNumber() {
        for (int attempt = 0; attempt < TRACKING_MAX_ATTEMPTS; attempt++) {
            StringBuilder builder = new StringBuilder(TRACKING_PREFIX);
            for (int i = 0; i < TRACKING_BODY_LENGTH; i++) {
                builder.append(TRACKING_ALPHABET.charAt(random.nextInt(TRACKING_ALPHABET.length())));
            }
            String candidate = builder.toString();
            if (!shipmentRepository.existsByTrackingNumber(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not generate a unique tracking number, please retry");
    }

    /* ===================== mapping ===================== */

    private ShipmentPackage toPackageEntity(PackageRequest request, int packageNo) {
        return ShipmentPackage.builder()
                .packageNo(packageNo)
                .description(request.getDescription())
                .weightKg(request.getWeightKg())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .quantity(request.getQuantity())
                .declaredValue(request.getDeclaredValue())
                .fragile(Boolean.TRUE.equals(request.getFragile()))
                .build();
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        List<PackageResponse> packages = shipment.getPackages().stream()
                .map(p -> PackageResponse.builder()
                        .id(p.getId())
                        .packageNo(p.getPackageNo())
                        .description(p.getDescription())
                        .weightKg(p.getWeightKg())
                        .lengthCm(p.getLengthCm())
                        .widthCm(p.getWidthCm())
                        .heightCm(p.getHeightCm())
                        .quantity(p.getQuantity())
                        .declaredValue(p.getDeclaredValue())
                        .fragile(p.getFragile())
                        .build())
                .toList();

        Set<String> nextStatuses = shipment.getStatus().nextStates().stream()
                .map(Enum::name)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        User creator = shipment.getCreatedBy();
        User operator = shipment.getAssignedOperator();

        return ShipmentResponse.builder()
                .id(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .createdById(creator.getId())
                .createdByName(creator.getFullName())
                .createdByRole(creator.getRole())
                .businessId(shipment.getBusinessId())
                .assignedOperatorId(operator == null ? null : operator.getId())
                .assignedOperatorName(operator == null ? null : operator.getFullName())
                .senderName(shipment.getSenderName())
                .senderPhone(shipment.getSenderPhone())
                .senderAddress(shipment.getSenderAddress())
                .receiverName(shipment.getReceiverName())
                .receiverPhone(shipment.getReceiverPhone())
                .receiverEmail(shipment.getReceiverEmail())
                .receiverAddress(shipment.getReceiverAddress())
                .pickupAddress(shipment.getPickupAddress())
                .deliveryAddress(shipment.getDeliveryAddress())
                .status(shipment.getStatus().name())
                .priority(shipment.getPriority().name())
                .allowedNextStatuses(nextStatuses)
                .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                .actualDeliveryDate(shipment.getActualDeliveryDate())
                .packages(packages)
                .totalPackages(packages.size())
                .cancelledAt(shipment.getCancelledAt())
                .cancellationReason(shipment.getCancellationReason())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}
