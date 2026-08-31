package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.service.AlertSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Sends alert SMS through the Twilio REST API.
 *
 * Called directly over the JDK HTTP client rather than through the Twilio SDK:
 * one form POST does not justify another dependency, and it keeps the timeout
 * and failure handling in plain sight.
 *
 * With no credentials configured this reports itself unconfigured and sends
 * nothing, so a local checkout works without a Twilio account.
 */
@Component("smsAlertSender")
@Slf4j
public class TwilioSmsAlertSender implements AlertSender {

    /** Twilio truncates long bodies into several billed segments. */
    private static final int MAX_BODY_LENGTH = 300;

    private final HttpClient httpClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String apiBaseUrl;
    private final boolean enabled;

    public TwilioSmsAlertSender(@Value("${twilio.account-sid:}") String accountSid,
                                @Value("${twilio.auth-token:}") String authToken,
                                @Value("${twilio.from-number:}") String fromNumber,
                                @Value("${twilio.api-base-url:https://api.twilio.com/2010-04-01}") String apiBaseUrl,
                                @Value("${notifications.sms.enabled:true}") boolean enabled,
                                @Value("${notifications.sms.timeout-ms:5000}") long timeoutMs) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.apiBaseUrl = apiBaseUrl;
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return enabled
                && accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && fromNumber != null && !fromNumber.isBlank();
    }

    @Override
    public boolean send(String destination, String subject, String body) {
        if (destination == null || destination.isBlank() || !isConfigured()) {
            return false;
        }

        String text = subject == null || subject.isBlank() ? body : subject + ": " + body;
        if (text.length() > MAX_BODY_LENGTH) {
            text = text.substring(0, MAX_BODY_LENGTH - 1) + "…";
        }

        try {
            String form = "To=" + encode(destination)
                    + "&From=" + encode(fromNumber)
                    + "&Body=" + encode(text);
            String credentials = Base64.getEncoder()
                    .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/Accounts/" + accountSid + "/Messages.json"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }
            // the body can contain the recipient's number, so log the status only
            log.warn("Twilio rejected the alert SMS with status {}", response.statusCode());
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while sending an alert SMS");
            return false;
        } catch (Exception ex) {
            log.warn("Could not send the alert SMS: {}", ex.getMessage());
            return false;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
