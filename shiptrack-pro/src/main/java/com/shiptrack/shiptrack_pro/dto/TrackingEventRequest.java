package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * A checkpoint an operator adds to the shipment timeline by hand, e.g.
 * "reached Vijayawada hub". It does not change the shipment status unless a
 * status is supplied and allowed by the lifecycle.
 */
@Data
public class TrackingEventRequest {

    @NotNull(message = "Shipment id is required")
    private Long shipmentId;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @DecimalMin(value = "-90.000000", message = "Latitude must be at least -90")
    @DecimalMax(value = "90.000000", message = "Latitude must be at most 90")
    @Digits(integer = 3, fraction = 6, message = "Latitude can have up to 6 decimal places")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.000000", message = "Longitude must be at least -180")
    @DecimalMax(value = "180.000000", message = "Longitude must be at most 180")
    @Digits(integer = 3, fraction = 6, message = "Longitude can have up to 6 decimal places")
    private BigDecimal longitude;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
