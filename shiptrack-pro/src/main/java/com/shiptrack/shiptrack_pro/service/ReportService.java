package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.reports.GeneratedReport;

public interface ReportService {

    GeneratedReport generateShipmentReport(String requesterEmail, String format);

    GeneratedReport generateDeliveryReport(String requesterEmail, String format);

    GeneratedReport generateRoutePerformanceReport(String requesterEmail, String format);

    GeneratedReport generateDelayAnalysisReport(String requesterEmail, String format);
}
