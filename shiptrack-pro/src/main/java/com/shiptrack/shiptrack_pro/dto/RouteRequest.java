package com.shiptrack.shiptrack_pro.dto;

import lombok.Data;

@Data
public class RouteRequest {

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