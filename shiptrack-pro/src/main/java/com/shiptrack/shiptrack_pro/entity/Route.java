package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "location_notes")
    private String locationNotes;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;
    
    @Builder.Default
    @Column(nullable = false)
    private String status = "PLANNED";

    // Many routes can belong to one shipment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(columnDefinition = "TEXT")
    private String waypoints;

    private Double distanceKm;

    private Integer estimatedTimeMinutes;

    private Integer actualTimeMinutes;

    private String trafficCondition;

    // Driver's last known location
    private Double lastKnownLatitude;

    private Double lastKnownLongitude;

    // Driver assigned by Operator/Admin
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;
}