package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.NotificationPreferenceRequest;
import com.shiptrack.shiptrack_pro.dto.NotificationPreferenceResponse;
import com.shiptrack.shiptrack_pro.dto.NotificationResponse;
import com.shiptrack.shiptrack_pro.entity.DelayRiskLevel;
import com.shiptrack.shiptrack_pro.entity.Shipment;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationService {

    /* ---- raising alerts (called from the write paths) ---- */

    /** The shipment moved to a new status. */
    void notifyStatusChange(Shipment shipment, String note, String actorName);

    /** The forecast now puts this shipment at risk of missing its promise. */
    void notifyDelayRisk(Shipment shipment, DelayRiskLevel level, int riskScore,
                         LocalDateTime predictedDeliveryAt, Integer expectedDelayMinutes);

    /* ---- reading (the signed-in user's own inbox) ---- */

    List<NotificationResponse> list(boolean unreadOnly, int limit);

    long unreadCount();

    NotificationResponse markRead(Long notificationId);

    int markAllRead();

    /* ---- settings ---- */

    NotificationPreferenceResponse getPreferences();

    NotificationPreferenceResponse updatePreferences(NotificationPreferenceRequest request);
}
