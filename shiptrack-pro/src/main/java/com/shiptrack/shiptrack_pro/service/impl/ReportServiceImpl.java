package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.reports.GeneratedReport;
import com.shiptrack.shiptrack_pro.entity.ProofOfDelivery;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ProofOfDeliveryRepository;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.ReportService;
import com.shiptrack.shiptrack_pro.service.support.DeliveryMetrics;
import com.shiptrack.shiptrack_pro.service.support.ReportGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final ProofOfDeliveryRepository podRepository;
    private final ReportGenerator reportGenerator;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String PDF = "pdf";
    private static final String EXCEL = "excel";

    // =========================================================
    // SHIPMENT REPORT
    // =========================================================
    @Override
    public GeneratedReport generateShipmentReport(String requesterEmail, String format) {

        List<Shipment> shipments = scopedShipments(requesterEmail);

        List<String> headers = List.of(
                "Tracking Number", "Sender", "Sender Address", "Receiver",
                "Receiver Address", "Receiver Phone", "Package", "Weight (kg)",
                "Status", "Created At"
        );

        List<List<String>> rows = new ArrayList<>();
        for (Shipment s : shipments) {
            rows.add(List.of(
                    s.getTrackingNumber(),
                    s.getSenderName(),
                    s.getSenderAddress(),
                    s.getReceiverName(),
                    s.getReceiverAddress(),
                    s.getReceiverPhone(),
                    s.getPackageDescription(),
                    String.valueOf(s.getWeightKg()),
                    s.getStatus(),
                    format(s.getCreatedAt())
            ));
        }

        return render("shipment-report", "Shipment Report", headers, rows, format);
    }

    // =========================================================
    // DELIVERY REPORT
    // =========================================================
    @Override
    public GeneratedReport generateDeliveryReport(String requesterEmail, String format) {

        List<Shipment> delivered = scopedShipments(requesterEmail).stream()
                .filter(DeliveryMetrics::isDelivered)
                .toList();

        List<String> headers = List.of(
                "Tracking Number", "Receiver", "Actual Delivery Date",
                "Proof of Delivery Status", "Verified By"
        );

        List<List<String>> rows = new ArrayList<>();
        for (Shipment s : delivered) {
            Optional<ProofOfDelivery> pod = podRepository.findByShipmentId(s.getId());

            rows.add(List.of(
                    s.getTrackingNumber(),
                    s.getReceiverName(),
                    pod.map(ProofOfDelivery::getVerifiedAt).map(this::format).orElse("-"),
                    pod.map(ProofOfDelivery::getStatus).orElse("NOT_SUBMITTED"),
                    pod.map(ProofOfDelivery::getVerifiedBy)
                            .map(User::getFullName).orElse("-")
            ));
        }

        return render("delivery-report", "Delivery Report", headers, rows, format);
    }

    // =========================================================
    // ROUTE PERFORMANCE REPORT
    // =========================================================
    @Override
    public GeneratedReport generateRoutePerformanceReport(String requesterEmail, String format) {

        List<Shipment> shipments = scopedShipments(requesterEmail);

        List<String> headers = List.of(
                "Tracking Number", "Origin", "Destination", "Distance (km)",
                "Estimated Time (min)", "Actual Time (min)", "Driver"
        );

        List<List<String>> rows = new ArrayList<>();
        for (Shipment s : shipments) {
            Route route = routeRepository.findByShipmentId(s.getId()).orElse(null);
            if (route == null) continue;

            rows.add(List.of(
                    s.getTrackingNumber(),
                    route.getOrigin(),
                    route.getDestination(),
                    route.getDistanceKm() != null ? String.valueOf(route.getDistanceKm()) : "-",
                    route.getEstimatedTimeMinutes() != null ? String.valueOf(route.getEstimatedTimeMinutes()) : "-",
                    route.getActualTimeMinutes() != null ? String.valueOf(route.getActualTimeMinutes()) : "-",
                    route.getDriver() != null ? route.getDriver().getFullName() : "Unassigned"
            ));
        }

        return render("route-performance-report", "Route Performance Report", headers, rows, format);
    }

    // =========================================================
    // DELAY ANALYSIS REPORT
    // =========================================================
    @Override
    public GeneratedReport generateDelayAnalysisReport(String requesterEmail, String format) {

        List<Shipment> shipments = scopedShipments(requesterEmail);

        List<String> headers = List.of(
                "Tracking Number", "Estimated Time (min)", "Actual Time (min)",
                "Delay (min)", "Delay Status"
        );

        List<List<String>> rows = new ArrayList<>();
        for (Shipment s : shipments) {
            Route route = routeRepository.findByShipmentId(s.getId()).orElse(null);
            if (route == null) continue;

            Integer delay = DeliveryMetrics.delayMinutes(route);
            Boolean onTime = DeliveryMetrics.isOnTime(route);
            String delayStatus = onTime == null ? "UNKNOWN" : (onTime ? "ON_TIME" : "DELAYED");

            rows.add(List.of(
                    s.getTrackingNumber(),
                    route.getEstimatedTimeMinutes() != null ? String.valueOf(route.getEstimatedTimeMinutes()) : "-",
                    route.getActualTimeMinutes() != null ? String.valueOf(route.getActualTimeMinutes()) : "-",
                    delay != null ? String.valueOf(delay) : "-",
                    delayStatus
            ));
        }

        return render("delay-analysis-report", "Delay Analysis Report", headers, rows, format);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    // Role + ownership scoping: CUSTOMER and BUSINESS_CLIENT only ever see
    // shipments where they are the owning "customer" record; ADMINISTRATOR
    // sees everything. Reuses the same repository call the Analytics
    // service uses, rather than a separate query.
    private List<Shipment> scopedShipments(String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found: " + requesterEmail));

        if ("ADMINISTRATOR".equals(requester.getRole())) {
            return shipmentRepository.findAll();
        }

        return shipmentRepository.findByCustomer(requester);
    }

    private GeneratedReport render(
            String baseFilename, String title,
            List<String> headers, List<List<String>> rows, String format) {

        String normalizedFormat = format == null ? "" : format.trim().toLowerCase();

        if (PDF.equals(normalizedFormat)) {
            return GeneratedReport.builder()
                    .content(reportGenerator.toPdf(title, headers, rows))
                    .filename(baseFilename + ".pdf")
                    .contentType("application/pdf")
                    .build();
        }

        if (EXCEL.equals(normalizedFormat)) {
            return GeneratedReport.builder()
                    .content(reportGenerator.toExcel(title, headers, rows))
                    .filename(baseFilename + ".xlsx")
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .build();
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported format '" + format + "'. Use 'pdf' or 'excel'."
        );
    }

    private String format(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_FMT);
    }
}
