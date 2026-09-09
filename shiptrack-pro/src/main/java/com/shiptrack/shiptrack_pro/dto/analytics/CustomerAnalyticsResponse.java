package com.shiptrack.shiptrack_pro.dto.analytics;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalyticsResponse {

    private long activeShipmentCount;
    private long totalShipmentCount;

    // e.g. { "CREATED": 2, "IN_TRANSIT": 1, "DELIVERED": 5 }
    private Map<String, Long> statusBreakdown;

    private List<ShipmentHistoryItem> shipmentHistory;

    private TrackingInsights trackingInsights;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentHistoryItem {
        private String trackingNumber;
        private String status;
        private String receiverName;
        private java.time.LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingInsights {
        private String mostRecentStatus;
        private String mostRecentTrackingNumber;
        private Double onTimeDeliveryRatePercent; // null if no delivered shipments yet
        private Double averageDeliveryTimeHours;  // null if not computable
    }
}
