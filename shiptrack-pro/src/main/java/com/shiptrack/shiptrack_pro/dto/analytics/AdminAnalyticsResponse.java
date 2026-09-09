package com.shiptrack.shiptrack_pro.dto.analytics;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsResponse {

    private UserSummary userSummary;
    private ShipmentMonitoring shipmentMonitoring;
    private DeliveryAnalytics deliveryAnalytics;
    private RoutePerformance routePerformance;
    private SystemMonitoring systemMonitoring;
    private ReportsManagement reportsManagement;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private long totalUsers;
        private Map<String, Long> usersByRole;
        private long newUsersLast30Days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentMonitoring {
        private long totalShipments;
        private Map<String, Long> shipmentsByStatus;
        private long shipmentsCreatedLast7Days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAnalytics {
        private long totalDelivered;
        private long totalRejected;
        private Double onTimeDeliveryRatePercent;
        private Double averageDeliveryTimeHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutePerformance {
        private long totalRoutes;
        private Double averageDistanceKm;
        private Double averageEstimatedTimeMinutes;
        private Double averageActualTimeMinutes;
        private Double averageDelayMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMonitoring {
        private long pendingPodVerifications;
        private long activeDriversAssigned;
        private long routesInProgress; // has a route but shipment not yet delivered/rejected
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportsManagement {
        private List<AvailableReport> availableReports;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AvailableReport {
            private String reportType;   // e.g. "shipments"
            private String displayName;  // e.g. "Shipment Report"
            private long recordCount;
        }
    }
}
