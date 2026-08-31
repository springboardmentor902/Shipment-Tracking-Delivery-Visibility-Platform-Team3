package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private boolean inAppEnabled;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean notifyStatusChange;
    private boolean notifyDelayRisk;
    private boolean notifyDelivery;
    private String minRiskLevel;

    /** So the UI can explain why a channel is switched off server-side. */
    private boolean emailChannelAvailable;
    private boolean smsChannelAvailable;
    private String phone;
}
