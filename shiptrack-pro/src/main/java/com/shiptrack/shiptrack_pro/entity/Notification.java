package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    // Required by the database
    @Column(nullable = false)
    private String status;

    @Column(name = "delivery_status", nullable = false)
    private String deliveryStatus;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }

        if (status == null) {
            status = "UNREAD";
        }

        if (deliveryStatus == null) {
            deliveryStatus = "SENT";
        }
    }
}