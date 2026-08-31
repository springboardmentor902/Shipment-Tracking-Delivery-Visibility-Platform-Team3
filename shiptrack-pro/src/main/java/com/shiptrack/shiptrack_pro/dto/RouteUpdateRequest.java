package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload for PUT /api/routes/{id} — partial update of one route leg,
 * including driver reassignment. Only the fields you send are applied.
 * The shipment a leg belongs to can never be changed.
 */
@Data
public class RouteUpdateRequest {

    @Positive(message = "Leg number must be greater than zero")
    private Integer legNumber;

    /** New driver for this leg. Must be a LOGISTICS_OPERATOR. */
    private Long driverId;

    @Size(max = 500, message = "Origin address must not exceed 500 characters")
    private String originAddress;

    @Size(max = 500, message = "Destination address must not exceed 500 characters")
    private String destinationAddress;

    @Size(max = 1000, message = "Waypoints must not exceed 1000 characters")
    private String waypoints;

    @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
    @DecimalMax(value = "90.0", message = "Latitude must be at most 90")
    @Digits(integer = 3, fraction = 6, message = "Latitude can have up to 6 decimal places")
    private BigDecimal originLatitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
    @DecimalMax(value = "180.0", message = "Longitude must be at most 180")
    @Digits(integer = 3, fraction = 6, message = "Longitude can have up to 6 decimal places")
    private BigDecimal originLongitude;

    @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
    @DecimalMax(value = "90.0", message = "Latitude must be at most 90")
    @Digits(integer = 3, fraction = 6, message = "Latitude can have up to 6 decimal places")
    private BigDecimal destinationLatitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
    @DecimalMax(value = "180.0", message = "Longitude must be at most 180")
    @Digits(integer = 3, fraction = 6, message = "Longitude can have up to 6 decimal places")
    private BigDecimal destinationLongitude;

    @DecimalMin(value = "0.01", message = "Distance must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Distance can have up to 2 decimal places")
    private BigDecimal distanceKm;

    @Positive(message = "Expected duration must be greater than zero")
    private Integer expectedDurationMinutes;

    @Positive(message = "Traffic duration must be greater than zero")
    private Integer durationInTrafficMinutes;

    @Size(max = 64, message = "Traffic condition must not exceed 64 characters")
    private String trafficCondition;

    /** PLANNED, ACTIVE, COMPLETED or SKIPPED. */
    @Size(max = 16, message = "Status must not exceed 16 characters")
    private String status;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
