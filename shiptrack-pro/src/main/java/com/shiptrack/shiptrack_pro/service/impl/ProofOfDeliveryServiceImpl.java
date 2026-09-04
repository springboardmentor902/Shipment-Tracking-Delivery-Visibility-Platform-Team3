package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.entity.ProofOfDelivery;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ProofOfDeliveryRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.FileUploadService;
import com.shiptrack.shiptrack_pro.service.ProofOfDeliveryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProofOfDeliveryServiceImpl
        implements ProofOfDeliveryService {

    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    // ==========================================
    // Create Proof of Delivery
    // ==========================================

    @Override
    public ProofOfDelivery createProofOfDelivery(
            Long shipmentId,
            Long verifiedById,
            MultipartFile signature,
            MultipartFile photo,
            String deliveredToName,
            String deliveryNotes) {

        // Find shipment
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + shipmentId
                ));

        // Find operator
        User verifiedBy = userRepository.findById(verifiedById)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with id: " + verifiedById
                ));

        // Prevent duplicate POD
        if (proofOfDeliveryRepository
                .findByShipmentId(shipmentId)
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Proof of delivery already exists for shipment: "
                            + shipmentId
            );
        }

        // ==========================================
        // Upload signature
        // ==========================================

        String signatureUrl = null;

        if (signature != null && !signature.isEmpty()) {
            signatureUrl = fileUploadService.uploadFile(signature);
        }

        // ==========================================
        // Upload delivery photo
        // ==========================================

        String photoUrl = null;

        if (photo != null && !photo.isEmpty()) {
            photoUrl = fileUploadService.uploadFile(photo);
        }

        // ==========================================
        // Create POD
        // ==========================================

        ProofOfDelivery pod = ProofOfDelivery.builder()
                .shipment(shipment)
                .verifiedBy(verifiedBy)
                .signatureUrl(signatureUrl)
                .photoUrl(photoUrl)
                .deliveredToName(deliveredToName)
                .deliveryNotes(deliveryNotes)
                .verificationStatus("PENDING")
                .deliveredAt(LocalDateTime.now())
                .build();

        // Save POD
        ProofOfDelivery savedPod =
                proofOfDeliveryRepository.save(pod);

        // ==========================================
        // Update shipment
        // ==========================================

        shipment.setStatus("DELIVERED");

        shipmentRepository.save(shipment);

        return savedPod;
    }

    // ==========================================
    // Get POD
    // ==========================================

    @Override
    public ProofOfDelivery getByShipmentId(Long shipmentId) {

        return proofOfDeliveryRepository
                .findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Proof of delivery not found for shipment: "
                                + shipmentId
                ));
    }

    // ==========================================
    // Verify POD
    // ==========================================

    @Override
    public ProofOfDelivery verify(
            Long shipmentId,
            Long verifiedById) {

        ProofOfDelivery pod =
                proofOfDeliveryRepository
                        .findByShipmentId(shipmentId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Proof of delivery not found"
                        ));

        User verifiedBy = userRepository.findById(verifiedById)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with id: " + verifiedById
                ));

        // Record verification
        pod.setVerifiedBy(verifiedBy);
        pod.setVerificationStatus("VERIFIED");
        pod.setVerifiedAt(LocalDateTime.now());

        return proofOfDeliveryRepository.save(pod);
    }
}