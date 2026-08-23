package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationRequest {

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;
}