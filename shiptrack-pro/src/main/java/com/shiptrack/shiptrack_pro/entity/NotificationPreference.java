package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    private boolean inAppEnabled = true;

    @Builder.Default
    private boolean emailEnabled = true;

    @Builder.Default
    private boolean smsEnabled = false;

    @Builder.Default
    private boolean notifyStatusChange = true;

    @Builder.Default
    private boolean notifyDelayRisk = true;

    @Builder.Default
    private boolean notifyDelivery = true;

    @Builder.Default
    @Column(name = "min_risk_level", nullable = false)
    private String minRiskLevel = "HIGH";
}