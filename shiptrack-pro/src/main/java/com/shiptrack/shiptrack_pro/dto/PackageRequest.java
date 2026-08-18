package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageRequest {

    @NotBlank(message = "Package description is required")
    private String description;

    @NotNull(message = "Package weight is required")
    @DecimalMin(value = "0.001", message = "Weight must be greater than zero")
    private BigDecimal weightKg;

    @DecimalMin(value = "0.0", inclusive = false, message = "Length must be greater than zero")
    private BigDecimal lengthCm;

    @DecimalMin(value = "0.0", inclusive = false, message = "Width must be greater than zero")
    private BigDecimal widthCm;

    @DecimalMin(value = "0.0", inclusive = false, message = "Height must be greater than zero")
    private BigDecimal heightCm;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @DecimalMin(value = "0.0", message = "Declared value cannot be negative")
    private BigDecimal declaredValue;

    private Boolean fragile;
}