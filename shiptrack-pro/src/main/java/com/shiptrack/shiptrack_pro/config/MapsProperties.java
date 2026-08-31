package com.shiptrack.shiptrack_pro.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Google Maps configuration.
 *
 * The API key is read from the environment (GOOGLE_MAPS_API_KEY) and is never
 * committed. When the key is missing the application still starts and every
 * Maps-backed feature degrades gracefully instead of failing the request.
 */
@Component
@Getter
public class MapsProperties {

    @Value("${google.maps.api-key:}")
    private String apiKey;

    @Value("${google.maps.geocode-url:https://maps.googleapis.com/maps/api/geocode/json}")
    private String geocodeUrl;

    @Value("${google.maps.directions-url:https://maps.googleapis.com/maps/api/directions/json}")
    private String directionsUrl;

    /** Per-request timeout in milliseconds, kept short so the API stays responsive. */
    @Value("${google.maps.timeout-ms:5000}")
    private int timeoutMs;

    /** True only when a key is actually configured. */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
