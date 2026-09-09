package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.PodResponse;
import com.shiptrack.shiptrack_pro.dto.PodSubmitRequest;
import com.shiptrack.shiptrack_pro.dto.PodSummaryResponse;
import com.shiptrack.shiptrack_pro.dto.PodVerifyRequest;
import com.shiptrack.shiptrack_pro.entity.ProofOfDelivery;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ProofOfDeliveryRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.PodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PodServiceImpl implements PodService {

    private final ProofOfDeliveryRepository podRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    // =========================================================
    // DRIVER SUBMITS PROOF OF DELIVERY
    // =========================================================
    @Override
    public PodResponse submitProof(Long shipmentId, PodSubmitRequest request, String driverEmail) {

        Shipment shipment = getShipmentOrThrow(shipmentId);

        if (podRepository.findByShipmentId(shipmentId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Proof of delivery already submitted for this shipment"
            );
        }

        User driver = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found: " + driverEmail
                ));

        ProofOfDelivery pod = ProofOfDelivery.builder()
                .shipment(shipment)
                .submittedBy(driver)
                .signatureImage(request.getSignatureImage())
                .photoImage(request.getPhotoImage())
                .receiverName(request.getReceiverName())
                .notes(request.getNotes())
                .status("PENDING")
                .build();

        ProofOfDelivery saved = podRepository.save(pod);

        // Shipment now awaits verification of the delivery proof
        shipment.setStatus("PENDING_VERIFICATION");
        shipmentRepository.save(shipment);

        return mapToResponse(saved);
    }

    // =========================================================
    // VERIFICATION QUEUE - all PENDING proofs
    // =========================================================
    @Override
    public List<PodSummaryResponse> getPendingQueue() {
        return podRepository.findByStatusOrderBySubmittedAtAsc("PENDING")
                .stream()
                .map(this::mapToSummary)
                .toList();
    }

    // =========================================================
    // FULL DETAIL FOR ONE PROOF (signature + photo)
    // =========================================================
    @Override
    public PodResponse getProofByShipmentId(Long shipmentId) {
        ProofOfDelivery pod = getPodOrThrow(shipmentId);
        return mapToResponse(pod);
    }

    // =========================================================
    // APPROVE / REJECT
    // =========================================================
    @Override
    public PodResponse verifyProof(Long shipmentId, PodVerifyRequest request, String verifierEmail) {

        ProofOfDelivery pod = getPodOrThrow(shipmentId);

        if (!"PENDING".equals(pod.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This proof has already been " + pod.getStatus().toLowerCase()
            );
        }

        if ("REJECTED".equals(request.getDecision())
                && (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "rejectionReason is required when rejecting a proof"
            );
        }

        User verifier = userRepository.findByEmail(verifierEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found: " + verifierEmail
                ));

        pod.setStatus(request.getDecision());
        pod.setVerifiedBy(verifier);
        pod.setVerifiedAt(LocalDateTime.now());
        pod.setRejectionReason(
                "REJECTED".equals(request.getDecision()) ? request.getRejectionReason() : null
        );

        ProofOfDelivery saved = podRepository.save(pod);

        // Reflect the verification decision back onto the shipment
        Shipment shipment = pod.getShipment();
        shipment.setStatus("APPROVED".equals(request.getDecision()) ? "DELIVERED" : "DELIVERY_REJECTED");
        shipmentRepository.save(shipment);

        return mapToResponse(saved);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Shipment getShipmentOrThrow(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found: " + shipmentId
                ));
    }

    private ProofOfDelivery getPodOrThrow(Long shipmentId) {
        return podRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No proof of delivery found for shipment: " + shipmentId
                ));
    }

    private PodSummaryResponse mapToSummary(ProofOfDelivery pod) {
        return PodSummaryResponse.builder()
                .id(pod.getId())
                .shipmentId(pod.getShipment().getId())
                .trackingNumber(pod.getShipment().getTrackingNumber())
                .receiverName(pod.getReceiverName())
                .submittedByName(pod.getSubmittedBy() != null ? pod.getSubmittedBy().getFullName() : null)
                .status(pod.getStatus())
                .submittedAt(pod.getSubmittedAt())
                .build();
    }

    private PodResponse mapToResponse(ProofOfDelivery pod) {
        return PodResponse.builder()
                .id(pod.getId())
                .shipmentId(pod.getShipment().getId())
                .trackingNumber(pod.getShipment().getTrackingNumber())
                .receiverName(pod.getReceiverName())
                .notes(pod.getNotes())
                .signatureImage(pod.getSignatureImage())
                .photoImage(pod.getPhotoImage())
                .submittedByName(pod.getSubmittedBy() != null ? pod.getSubmittedBy().getFullName() : null)
                .status(pod.getStatus())
                .submittedAt(pod.getSubmittedAt())
                .verifiedByName(pod.getVerifiedBy() != null ? pod.getVerifiedBy().getFullName() : null)
                .verifiedAt(pod.getVerifiedAt())
                .rejectionReason(pod.getRejectionReason())
                .build();
    }
}
