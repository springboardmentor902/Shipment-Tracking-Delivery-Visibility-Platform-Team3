package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Per-user alert settings.
 *
 * Defaults are deliberately conservative: in-app and email on, SMS off. SMS
 * costs money and interrupts people, so it is opt-in.
 */
@Entity
@Table(name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_pref_user", columnNames = "user_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /* ---- channels ---- */

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private boolean smsEnabled = false;

    /* ---- topics ---- */

    @Column(name = "notify_status_change", nullable = false)
    @Builder.Default
    private boolean notifyStatusChange = true;

    @Column(name = "notify_delay_risk", nullable = false)
    @Builder.Default
    private boolean notifyDelayRisk = true;

    @Column(name = "notify_delivery", nullable = false)
    @Builder.Default
    private boolean notifyDelivery = true;

    /**
     * Delay alerts below this level are not sent at all. HIGH by default, so a
     * shipment merely worth watching does not wake anyone.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "min_risk_level", nullable = false, length = 16)
    @Builder.Default
    private DelayRiskLevel minRiskLevel = DelayRiskLevel.HIGH;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
