package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "shipment_id",
            nullable = false,
            unique = true
    )
    private Shipment shipment;

    @Column(name = "predicted_delivery_time")
    private LocalDateTime predictedDeliveryTime;

    @Column(name = "delay_risk_score")
    private Double delayRiskScore;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String factors;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
}