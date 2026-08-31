package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One leg of the journey a shipment takes (ROUTES table).
 *
 * A shipment can have many legs: pickup hub -> sorting hub -> destination city
 * -> receiver address. Each leg is ordered by {@code legNumber} and can be
 * driven by a different logistics operator.
 *
 * Distance, duration, coordinates and traffic condition are filled by the
 * Google Maps geocoding / directions integration; they stay null until a
 * provider lookup succeeds, so a route can always be created manually.
 */
@Entity
@Table(
        name = "routes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_routes_shipment_leg",
                columnNames = {"shipment_id", "leg_number"}
        ),
        indexes = {
                @Index(name = "idx_routes_shipment", columnList = "shipment_id"),
                @Index(name = "idx_routes_driver", columnList = "driver_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Many legs belong to one shipment. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    /** Order of this leg inside the shipment: 1, 2, 3 ... */
    @Column(name = "leg_number", nullable = false)
    private Integer legNumber;

    /** Logistics operator driving this leg. Assigned/reassigned by operator or admin. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    /* ---------- endpoints ---------- */

    @Column(name = "origin_address", nullable = false, length = 500)
    private String originAddress;

    @Column(name = "destination_address", nullable = false, length = 500)
    private String destinationAddress;

    @Column(name = "waypoints", length = 1000)
    private String waypoints;

    /* ---------- geocoded coordinates ---------- */

    @Column(name = "origin_latitude", precision = 9, scale = 6)
    private BigDecimal originLatitude;

    @Column(name = "origin_longitude", precision = 9, scale = 6)
    private BigDecimal originLongitude;

    @Column(name = "destination_latitude", precision = 9, scale = 6)
    private BigDecimal destinationLatitude;

    @Column(name = "destination_longitude", precision = 9, scale = 6)
    private BigDecimal destinationLongitude;

    /* ---------- directions / distance matrix results ---------- */

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "expected_duration_minutes")
    private Integer expectedDurationMinutes;

    /** Traffic-aware duration when the provider returns one. */
    @Column(name = "duration_in_traffic_minutes")
    private Integer durationInTrafficMinutes;

    /** Free text from the provider or the operator, e.g. LIGHT / MODERATE / HEAVY. */
    @Column(name = "traffic_condition", length = 64)
    private String trafficCondition;

    /* ---------- live position of the driver on this leg ---------- */

    @Column(name = "last_known_latitude", precision = 9, scale = 6)
    private BigDecimal lastKnownLatitude;

    @Column(name = "last_known_longitude", precision = 9, scale = 6)
    private BigDecimal lastKnownLongitude;

    @Column(name = "last_location_at")
    private LocalDateTime lastLocationAt;

    /* ---------- lifecycle ---------- */

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RouteLegStatus status = RouteLegStatus.PLANNED;

    @Column(name = "notes", length = 500)
    private String notes;

    /* ---------- audit ---------- */

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
