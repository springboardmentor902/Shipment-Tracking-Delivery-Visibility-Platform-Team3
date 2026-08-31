package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.service.AlertSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends alert emails over the configured SMTP server.
 *
 * The mail sender is injected lazily: with no SMTP settings in
 * application.properties the bean may be absent, and the app must still start
 * and work with in-app alerts only.
 */
@Component("emailAlertSender")
@Slf4j
public class EmailAlertSender implements AlertSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;
    private final boolean enabled;

    public EmailAlertSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                            @Value("${notifications.email.enabled:true}") boolean enabled,
                            @Value("${notifications.email.from:${spring.mail.username:}}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
    }

    @Override
    public boolean isConfigured() {
        return enabled && fromAddress != null && !fromAddress.isBlank()
                && mailSenderProvider.getIfAvailable() != null;
    }

    @Override
    public boolean send(String destination, String subject, String body) {
        if (destination == null || destination.isBlank() || !isConfigured()) {
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(destination);
            message.setSubject(subject);
            message.setText(body);
            mailSenderProvider.getObject().send(message);
            return true;
        } catch (RuntimeException ex) {
            // a dead SMTP server must not break the delivery event behind the alert
            log.warn("Could not email the alert to {}: {}", destination, ex.getMessage());
            return false;
        }
    }
}
