package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.service.impl.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/shipments")
    public ResponseEntity<byte[]> downloadShipments(
            @RequestParam String format) throws Exception {

        if ("pdf".equalsIgnoreCase(format)) {

            byte[] pdf = reportService.generatePdf();

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=shipment-report.pdf"
                    )
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        }

        if ("excel".equalsIgnoreCase(format)) {

            byte[] excel = reportService.generateExcel();

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=shipment-report.xlsx"
                    )
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    ))
                    .body(excel);
        }

        return ResponseEntity.badRequest().build();
    }
}
