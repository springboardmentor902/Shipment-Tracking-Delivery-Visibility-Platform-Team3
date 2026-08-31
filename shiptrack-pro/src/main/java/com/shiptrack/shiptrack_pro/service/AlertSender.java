package com.shiptrack.shiptrack_pro.service;

/**
 * One outbound alert channel.
 *
 * Implementations must never throw: a channel being misconfigured or offline is
 * not a reason to fail the delivery event that triggered the alert.
 */
public interface AlertSender {

    /** True when the channel has enough configuration to actually send. */
    boolean isConfigured();

    /**
     * @param destination email address or phone number
     * @return true when the message was handed off successfully
     */
    boolean send(String destination, String subject, String body);
}
