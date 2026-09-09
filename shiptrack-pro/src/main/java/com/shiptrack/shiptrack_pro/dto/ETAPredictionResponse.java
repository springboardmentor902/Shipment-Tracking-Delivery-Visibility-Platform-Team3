package com.shiptrack.shiptrack_pro.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ETAPredictionResponse {

    private Long id;
    private Long shipmentId;
    private LocalDateTime predictedDeliveryTime;
    private Double delayRiskScore;
    private Double confidenceScore;
    private String factors;
    private LocalDateTime calculatedAt;
}