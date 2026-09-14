package com.shiptrack.shiptrack_pro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteAlternativeDTO {

    private Double distanceKm;

    private Integer durationMinutes;

    private Integer trafficAdjustedDurationMinutes;

    private String routeSummary;
}