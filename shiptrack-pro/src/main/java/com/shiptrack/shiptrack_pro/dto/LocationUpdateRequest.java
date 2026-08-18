package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LocationUpdateRequest {

    @NotNull(message = "Shipment id is required")
    private Long shipmentId;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.000000", message = "Latitude must be at least -90")
    @DecimalMax(value = "90.000000", message = "Latitude must be at most 90")
    @Digits(integer = 3, fraction = 6, message = "Latitude can have up to 6 decimal places")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.000000", message = "Longitude must be at least -180")
    @DecimalMax(value = "180.000000", message = "Longitude must be at most 180")
    @Digits(integer = 3, fraction = 6, message = "Longitude can have up to 6 decimal places")
    private BigDecimal longitude;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
