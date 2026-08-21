package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;

    // The CUSTOMER (or BUSINESS_CLIENT) who created this shipment.
    // Set from the authenticated JWT identity in the service layer,
    // never trusted from the request body.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "sender_address", nullable = false)
    private String senderAddress;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_address", nullable = false)
    private String receiverAddress;

    @Column(name = "receiver_phone", nullable = false)
    private String receiverPhone;

    @Column(name = "package_description", nullable = false)
    private String packageDescription;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "status", nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
