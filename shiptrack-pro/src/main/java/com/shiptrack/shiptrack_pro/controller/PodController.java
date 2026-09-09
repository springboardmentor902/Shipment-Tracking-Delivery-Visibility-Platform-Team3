package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.PodResponse;
import com.shiptrack.shiptrack_pro.dto.PodSubmitRequest;
import com.shiptrack.shiptrack_pro.dto.PodSummaryResponse;
import com.shiptrack.shiptrack_pro.dto.PodVerifyRequest;
import com.shiptrack.shiptrack_pro.service.PodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pod")
@RequiredArgsConstructor
public class PodController {

    private final PodService podService;

    // =========================================================
    // DRIVER SUBMITS PROOF OF DELIVERY
    // POST /api/pod/{shipmentId}
    // =========================================================
    @PostMapping("/{shipmentId}")
    @PreAuthorize("hasRole('LOGISTICS_OPERATOR')")
    public ResponseEntity<PodResponse> submitProof(
            @PathVariable Long shipmentId,
            @Valid @RequestBody PodSubmitRequest request,
            Authentication authentication) {

        PodResponse response =
                podService.submitProof(shipmentId, request, authentication.getName());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================================================
    // VERIFICATION QUEUE - proofs pending verification
    // GET /api/pod/pending
    // =========================================================
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<List<PodSummaryResponse>> getPendingQueue() {
        return ResponseEntity.ok(podService.getPendingQueue());
    }

    // =========================================================
    // FULL DETAIL FOR ONE PROOF (signature + photo)
    // GET /api/pod/{shipmentId}
    // =========================================================
    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<PodResponse> getProof(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(podService.getProofByShipmentId(shipmentId));
    }

    // =========================================================
    // APPROVE / REJECT
    // PATCH /api/pod/{shipmentId}/verify
    // =========================================================
    @PatchMapping("/{shipmentId}/verify")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<PodResponse> verifyProof(
            @PathVariable Long shipmentId,
            @Valid @RequestBody PodVerifyRequest request,
            Authentication authentication) {

        PodResponse response =
                podService.verifyProof(shipmentId, request, authentication.getName());

        return ResponseEntity.ok(response);
    }
}
