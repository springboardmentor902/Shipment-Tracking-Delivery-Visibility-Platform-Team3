package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;

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

    // One shipment can have one route
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
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