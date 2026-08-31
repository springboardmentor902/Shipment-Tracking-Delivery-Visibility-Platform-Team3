package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The latest delivery forecast for one shipment.
 *
 * Exactly one row per shipment: every recalculation overwrites the previous
 * forecast, so reads never have to pick the newest of many rows.
 */
@Entity
@Table(name = "eta_predictions",
        uniqueConstraints = @UniqueConstraint(name = "uk_eta_shipment", columnNames = "shipment_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtaPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    /** When the parcel is now expected to arrive. */
    @Column(name = "predicted_delivery_at")
    private LocalDateTime predictedDeliveryAt;

    /** What the customer was promised, copied at calculation time. */
    @Column(name = "promised_delivery_date")
    private LocalDate promisedDeliveryDate;

    /** Positive means late, negative means early. */
    @Column(name = "expected_delay_minutes")
    private Integer expectedDelayMinutes;

    /** 0 (on track) to 100 (almost certainly late). */
    @Column(name = "delay_risk_score", nullable = false)
    private Integer delayRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private DelayRiskLevel riskLevel;

    /** How much the inputs can be trusted, 0-100. */
    @Column(name = "confidence_score", nullable = false)
    private Integer confidenceScore;

    /** Human readable reasons, one per line. */
    @Column(name = "factors", length = 1000)
    private String factors;

    /** ROUTE_METRICS when legs drove the estimate, STATUS_HEURISTIC otherwise. */
    @Column(name = "source", length = 32)
    private String source;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    @PreUpdate
    void stampCalculatedAt() {
        if (calculatedAt == null) {
            calculatedAt = LocalDateTime.now();
        }
    }
}
