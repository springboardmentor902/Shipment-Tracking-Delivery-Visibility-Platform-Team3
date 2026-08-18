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
    private String originAddress;
    private String destinationAddress;
    private String waypoints;
    private BigDecimal distanceKm;
    private Integer expectedDurationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
