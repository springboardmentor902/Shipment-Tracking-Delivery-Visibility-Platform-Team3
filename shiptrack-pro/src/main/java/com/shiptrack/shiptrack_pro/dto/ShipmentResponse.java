package com.shiptrack.shiptrack_pro.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
    private Long id;
    private String trackingNumber;
    private Long customerId;
    private String customerEmail;
    private String senderName;
    private String senderAddress;
    private String receiverName;
    private String receiverAddress;
    private String receiverPhone;
    private String packageDescription;
    private Double weightKg;
    private String status;
    private LocalDateTime createdAt;
}
