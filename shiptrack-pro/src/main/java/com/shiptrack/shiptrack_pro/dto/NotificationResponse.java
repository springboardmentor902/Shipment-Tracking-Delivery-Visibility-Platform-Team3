package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String type;
    private String severity;
    private String title;
    private String message;
    private Long shipmentId;
    private String trackingNumber;
    private boolean read;
    private LocalDateTime readAt;
    private boolean emailSent;
    private boolean smsSent;
    private LocalDateTime createdAt;
}
