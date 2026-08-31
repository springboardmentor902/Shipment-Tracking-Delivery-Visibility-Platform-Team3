package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Partial update: only the fields present are changed, so the UI can toggle one
 * switch without resending the whole set.
 */
@Data
public class NotificationPreferenceRequest {

    private Boolean inAppEnabled;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean notifyStatusChange;
    private Boolean notifyDelayRisk;
    private Boolean notifyDelivery;

    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL",
            message = "minRiskLevel must be LOW, MEDIUM, HIGH or CRITICAL")
    private String minRiskLevel;
}
