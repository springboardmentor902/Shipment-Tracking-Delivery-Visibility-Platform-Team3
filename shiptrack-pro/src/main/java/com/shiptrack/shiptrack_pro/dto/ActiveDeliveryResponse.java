package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveDeliveryResponse {

    private String trackingNumber;
    private Long shipmentId;
    private String status;
    private String priority;
    private String receiverName;
    private String deliveryAddress;
    private String assignedOperatorName;
    private LocalDate estimatedDeliveryDate;
    private String lastLocation;
    private BigDecimal lastLatitude;
    private BigDecimal lastLongitude;
    private LocalDateTime lastUpdatedAt;
    private boolean delayed;
}
