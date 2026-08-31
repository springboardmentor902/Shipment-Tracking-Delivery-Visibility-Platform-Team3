package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The delivery forecast as the UI needs it: when, how late, how risky, how sure,
 * and why.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtaResponse {

    private Long shipmentId;
    private String trackingNumber;
    private String status;
    private String priority;
    private String receiverName;
    private String deliveryAddress;
    private String assignedOperatorName;

    private LocalDateTime predictedDeliveryAt;
    private LocalDate promisedDeliveryDate;
    private Integer expectedDelayMinutes;
    private Integer delayRiskScore;
    private String riskLevel;
    private Integer confidenceScore;
    private List<String> factors;
    private String source;
    private LocalDateTime calculatedAt;
}
