package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RouteRequest {

    @NotNull(message = "Shipment id is required")
    private Long shipmentId;

    @NotBlank(message = "Origin address is required")
    @Size(max = 500, message = "Origin address must not exceed 500 characters")
    private String originAddress;

    @NotBlank(message = "Destination address is required")
    @Size(max = 500, message = "Destination address must not exceed 500 characters")
    private String destinationAddress;

    @Size(max = 1000, message = "Waypoints must not exceed 1000 characters")
    private String waypoints;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.01", message = "Distance must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Distance can have up to 2 decimal places")
    private BigDecimal distanceKm;

    @NotNull(message = "Expected duration is required")
    @Positive(message = "Expected duration must be greater than zero")
    private Integer expectedDurationMinutes;
}
