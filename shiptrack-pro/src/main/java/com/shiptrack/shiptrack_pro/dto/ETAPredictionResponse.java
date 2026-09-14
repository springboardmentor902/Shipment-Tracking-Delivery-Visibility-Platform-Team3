package com.shiptrack.shiptrack_pro.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ETAPredictionResponse {

    private Long id;

    private Long shipmentId;

    private String trackingNumber;

    private String status;

    private String receiverName;

    private LocalDateTime promisedDeliveryTime;

    private LocalDateTime predictedDeliveryTime;

    private Integer expectedDelayMinutes;

    private Double delayRiskScore;

    private Double confidenceScore;

    private String factors;

    private LocalDateTime calculatedAt;
}