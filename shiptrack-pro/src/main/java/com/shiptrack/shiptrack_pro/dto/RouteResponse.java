package com.shiptrack.shiptrack_pro.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteResponse {

    private Long id;

    private Long shipmentId;

    private String origin;

    private String destination;

    private String waypoints;

    private Double distanceKm;

    private Integer estimatedTimeMinutes;

    private Integer actualTimeMinutes;

    private String trafficCondition;

    private Long driverId;
}