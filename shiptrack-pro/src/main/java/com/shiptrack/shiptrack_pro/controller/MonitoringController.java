package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.ActiveDeliveryResponse;
import com.shiptrack.shiptrack_pro.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<List<ActiveDeliveryResponse>> getActiveDeliveries() {
        return ResponseEntity.ok(monitoringService.getActiveDeliveries());
    }
}
