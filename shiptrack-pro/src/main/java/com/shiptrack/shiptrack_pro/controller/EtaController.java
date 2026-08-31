package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.EtaResponse;
import com.shiptrack.shiptrack_pro.service.EtaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Delivery forecasts and the delay watch list.
 *
 * Reads are open to anyone who may already see the shipment; the access check
 * lives in the service so REST, the socket and the scheduler cannot disagree.
 */
@RestController
@RequestMapping("/api/eta")
@RequiredArgsConstructor
public class EtaController {

    private static final int DEFAULT_RISK_FLOOR = 50;

    private final EtaService etaService;

    @GetMapping("/shipments/{shipmentId}")
    public ResponseEntity<EtaResponse> getForShipment(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(etaService.getForShipment(shipmentId));
    }

    /** Forces a fresh forecast; useful right after a route or traffic change. */
    @PostMapping("/shipments/{shipmentId}/recalculate")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<EtaResponse> recalculate(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(etaService.recalculate(shipmentId));
    }

    /**
     * Shipments likely to miss their promise, scoped to what the caller may see:
     * a business client gets only its own, an operator only its assignments.
     */
    @GetMapping("/at-risk")
    public ResponseEntity<List<EtaResponse>> getAtRisk(
            @RequestParam(name = "minScore", required = false) Integer minScore) {
        return ResponseEntity.ok(etaService.getAtRisk(minScore == null ? DEFAULT_RISK_FLOOR : minScore));
    }
}
