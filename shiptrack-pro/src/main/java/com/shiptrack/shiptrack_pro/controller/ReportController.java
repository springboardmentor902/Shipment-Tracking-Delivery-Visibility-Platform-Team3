package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.reports.GeneratedReport;
import com.shiptrack.shiptrack_pro.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // GET /api/reports/shipments?format=pdf|excel
    @GetMapping("/shipments")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'ADMINISTRATOR')")
    public ResponseEntity<byte[]> getShipmentReport(
            @RequestParam String format,
            Authentication authentication) {
        return toResponse(reportService.generateShipmentReport(authentication.getName(), format));
    }

    // GET /api/reports/delivery?format=pdf|excel
    @GetMapping("/delivery")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'ADMINISTRATOR')")
    public ResponseEntity<byte[]> getDeliveryReport(
            @RequestParam String format,
            Authentication authentication) {
        return toResponse(reportService.generateDeliveryReport(authentication.getName(), format));
    }

    // GET /api/reports/route-performance?format=pdf|excel
    @GetMapping("/route-performance")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'ADMINISTRATOR')")
    public ResponseEntity<byte[]> getRoutePerformanceReport(
            @RequestParam String format,
            Authentication authentication) {
        return toResponse(reportService.generateRoutePerformanceReport(authentication.getName(), format));
    }

    // GET /api/reports/delay-analysis?format=pdf|excel
    @GetMapping("/delay-analysis")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'ADMINISTRATOR')")
    public ResponseEntity<byte[]> getDelayAnalysisReport(
            @RequestParam String format,
            Authentication authentication) {
        return toResponse(reportService.generateDelayAnalysisReport(authentication.getName(), format));
    }

    private ResponseEntity<byte[]> toResponse(GeneratedReport report) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(report.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + report.getFilename() + "\"")
                .body(report.getContent());
    }
}
