package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;

    private Long userId;

    private Long shipmentId;

    private String trackingNumber;

    private String type;

    private String title;

    private String message;

    private String status;

    private String deliveryStatus;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}