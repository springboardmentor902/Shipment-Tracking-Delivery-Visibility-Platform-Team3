package com.shiptrack.shiptrack_pro.entity;

/**
 * Buckets a 0-100 delay risk score so the UI can colour it consistently.
 */
public enum DelayRiskLevel {

    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static DelayRiskLevel fromScore(int score) {
        if (score >= 75) return CRITICAL;
        if (score >= 50) return HIGH;
        if (score >= 25) return MEDIUM;
        return LOW;
    }
}
