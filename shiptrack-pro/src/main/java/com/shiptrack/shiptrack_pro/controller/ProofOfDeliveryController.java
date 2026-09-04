package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.entity.ProofOfDelivery;
import com.shiptrack.shiptrack_pro.service.ProofOfDeliveryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pod")
@RequiredArgsConstructor
public class ProofOfDeliveryController {

    private final ProofOfDeliveryService proofOfDeliveryService;

    // ==========================================
    // Create Proof of Delivery
    // ==========================================

    @PostMapping("/{shipmentId}")
    public ResponseEntity<ProofOfDelivery> createPOD(

            @PathVariable Long shipmentId,

            @RequestParam Long verifiedById,

            @RequestParam(required = false)
            MultipartFile signature,

            @RequestParam(required = false)
            MultipartFile photo,

            @RequestParam String deliveredToName,

            @RequestParam(required = false)
            String deliveryNotes) {

        ProofOfDelivery pod =
                proofOfDeliveryService.createProofOfDelivery(
                        shipmentId,
                        verifiedById,
                        signature,
                        photo,
                        deliveredToName,
                        deliveryNotes
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pod);
    }

    // ==========================================
    // Get POD
    // ==========================================

    @GetMapping("/{shipmentId}")
    public ResponseEntity<ProofOfDelivery> getPOD(
            @PathVariable Long shipmentId) {

        return ResponseEntity.ok(
                proofOfDeliveryService
                        .getByShipmentId(shipmentId)
        );
    }

    // ==========================================
    // Verify POD
    // ==========================================

    @PatchMapping("/{shipmentId}/verify")
    public ResponseEntity<ProofOfDelivery> verifyPOD(

            @PathVariable Long shipmentId,

            @RequestParam Long verifiedById) {

        return ResponseEntity.ok(
                proofOfDeliveryService.verify(
                        shipmentId,
                        verifiedById
                )
        );
    }
}