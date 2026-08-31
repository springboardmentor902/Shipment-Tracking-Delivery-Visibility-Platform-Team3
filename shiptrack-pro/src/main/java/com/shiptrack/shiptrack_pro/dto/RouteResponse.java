package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {

    private Long id;
    private Long shipmentId;
    private String shipmentTrackingNumber;
    private Integer legNumber;

    private Long driverId;
    private String driverName;

    private String originAddress;
    private String destinationAddress;
    private String waypoints;

    private BigDecimal originLatitude;
    private BigDecimal originLongitude;
    private BigDecimal destinationLatitude;
    private BigDecimal destinationLongitude;

    private BigDecimal distanceKm;
    private Integer expectedDurationMinutes;
    private Integer durationInTrafficMinutes;
    private String trafficCondition;

    /** LIVE_MAPS, STRAIGHT_LINE or MANUAL — lets the UI label estimated values. */
    private String metricsSource;

    private BigDecimal lastKnownLatitude;
    private BigDecimal lastKnownLongitude;
    private LocalDateTime lastLocationAt;

    private String status;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
