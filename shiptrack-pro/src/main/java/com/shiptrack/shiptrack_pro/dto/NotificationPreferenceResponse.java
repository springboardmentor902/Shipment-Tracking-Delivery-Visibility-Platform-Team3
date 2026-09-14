package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceResponse {

    private boolean inAppEnabled;
    private boolean emailEnabled;
    private boolean smsEnabled;

    private boolean notifyStatusChange;
    private boolean notifyDelayRisk;
    private boolean notifyDelivery;

    private String minRiskLevel;

    private boolean emailChannelAvailable;
    private boolean smsChannelAvailable;
    private String phone;
}