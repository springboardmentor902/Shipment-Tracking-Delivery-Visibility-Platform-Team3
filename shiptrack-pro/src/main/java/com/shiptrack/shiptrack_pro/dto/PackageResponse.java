package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageResponse {
    private Long id;
    private Integer packageNo;
    private String description;
    private BigDecimal weightKg;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private Integer quantity;
    private BigDecimal declaredValue;
    private Boolean fragile;
}