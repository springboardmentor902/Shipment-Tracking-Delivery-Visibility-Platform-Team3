package com.shiptrack.shiptrack_pro.entity;

/**
 * What a notification is about. The type also decides which preference switch
 * and which channels apply.
 */
public enum NotificationType {

    STATUS_CHANGE,
    DELAY_RISK,
    ETA_CHANGE,
    DELIVERED,
    CANCELLED,
    POD_VERIFIED,
    GENERAL
}
