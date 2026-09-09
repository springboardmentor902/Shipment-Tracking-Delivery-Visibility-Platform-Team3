package com.shiptrack.shiptrack_pro.dto.analytics;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessAnalyticsResponse {

    private long totalShipments;
    private long activeShipments;
    private long deliveredShipments;

    // Shipment count for each status
    private Map<String, Long> statusBreakdown;

    private DeliveryPerformance deliveryPerformance;
    private DelayAnalysis delayAnalysis;
    private CustomerActivity customerActivity;
    private LogisticsOverview logisticsOverview;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryPerformance {
        private Double onTimeDeliveryRatePercent;
        private Double averageDeliveryTimeHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DelayAnalysis {
        private long delayedShipmentCount;
        private Double averageDelayMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerActivity {
        private long distinctReceiverCount;
        // top receivers by shipment volume, e.g. { "Jane Doe": 12, "Acme Co": 7 }
        private Map<String, Long> topReceivers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogisticsOverview {
        private Map<String, Long> shipmentCountByStatus;
        private Double totalDistanceCoveredKm;
        private long routesWithDriverAssigned;
        private long totalRoutes;
    }
}
