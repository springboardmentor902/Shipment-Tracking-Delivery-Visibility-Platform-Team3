package com.shiptrack.shiptrack_pro.service;

import tools.jackson.databind.ObjectMapper;
import com.shiptrack.shiptrack_pro.config.MapsProperties;
import com.shiptrack.shiptrack_pro.dto.GeoPoint;
import com.shiptrack.shiptrack_pro.service.impl.GoogleMapsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Maps provider must never break a request: no key, a dead endpoint or a
 * junk response all have to come back as an empty Optional.
 */
class GoogleMapsServiceTest {

    private static final GeoPoint HYDERABAD =
            new GeoPoint(new BigDecimal("17.385044"), new BigDecimal("78.486671"), null);
    private static final GeoPoint BENGALURU =
            new GeoPoint(new BigDecimal("12.971599"), new BigDecimal("77.594566"), null);

    private GoogleMapsService serviceWith(String apiKey, String baseUrl) {
        MapsProperties properties = new MapsProperties();
        ReflectionTestUtils.setField(properties, "apiKey", apiKey);
        ReflectionTestUtils.setField(properties, "geocodeUrl", baseUrl + "/geocode/json");
        ReflectionTestUtils.setField(properties, "directionsUrl", baseUrl + "/directions/json");
        ReflectionTestUtils.setField(properties, "timeoutMs", 800);
        return new GoogleMapsService(properties, new ObjectMapper());
    }

    @Test
    @DisplayName("without an API key the provider reports disabled and returns nothing")
    void disabledWithoutKey() {
        GoogleMapsService maps = serviceWith("", "https://maps.googleapis.com/maps/api");

        assertFalse(maps.isEnabled());
        assertTrue(maps.geocode("Hyderabad, Telangana").isEmpty());
        assertTrue(maps.routeMetrics(HYDERABAD, BENGALURU, null).isEmpty());
    }

    @Test
    @DisplayName("a blank address is never sent to the provider")
    void blankAddressReturnsEmpty() {
        GoogleMapsService maps = serviceWith("test-key", "http://127.0.0.1:1");

        assertTrue(maps.geocode(null).isEmpty());
        assertTrue(maps.geocode("   ").isEmpty());
    }

    @Test
    @DisplayName("an unreachable provider degrades to empty instead of throwing")
    void providerFailureIsSwallowed() {
        // port 1 is closed, so every call fails at the network level
        GoogleMapsService maps = serviceWith("test-key", "http://127.0.0.1:1");

        assertTrue(maps.isEnabled());
        assertTrue(maps.geocode("Hyderabad, Telangana").isEmpty());
        assertTrue(maps.routeMetrics(HYDERABAD, BENGALURU, "Kurnool;Anantapur").isEmpty());
    }

    @Test
    @DisplayName("missing coordinates never reach the directions call")
    void missingPointsReturnEmpty() {
        GoogleMapsService maps = serviceWith("test-key", "http://127.0.0.1:1");

        assertTrue(maps.routeMetrics(null, BENGALURU, null).isEmpty());
        assertTrue(maps.routeMetrics(HYDERABAD, null, null).isEmpty());
    }
}
