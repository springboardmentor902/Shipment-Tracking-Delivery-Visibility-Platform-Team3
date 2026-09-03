package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eta_predictions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ETAPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Prediction belongs to one shipment
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @Column(name = "predicted_delivery_time")
    private LocalDateTime predictedDeliveryTime;

    // Range: 0 to 10
    @Column(name = "delay_risk_score")
    private Double delayRiskScore;

    // Range: 0 to 100
    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String factors;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
}