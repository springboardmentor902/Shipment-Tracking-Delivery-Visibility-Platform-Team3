package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @Column(nullable = false, length = 500)
    private String originAddress;

    @Column(nullable = false, length = 500)
    private String destinationAddress;

    @Column(length = 1000)
    private String waypoints;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(nullable = false)
    private Integer expectedDurationMinutes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
