package com.shiptrack.shiptrack_pro.service.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.shiptrack.shiptrack_pro.config.MapsProperties;
import com.shiptrack.shiptrack_pro.dto.GeoPoint;
import com.shiptrack.shiptrack_pro.dto.RouteMetrics;
import com.shiptrack.shiptrack_pro.service.MapsService;
import com.shiptrack.shiptrack_pro.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Google Maps Geocoding + Directions client.
 *
 * Deliberately defensive: any missing key, network error, quota error or
 * unexpected payload results in Optional.empty() and a warning log, never an
 * exception that reaches the user.
 */
@Service
public class GoogleMapsService implements MapsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleMapsService.class);

    private final MapsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GoogleMapsService(MapsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /* ===================== geocoding ===================== */

    @Override
    public Optional<GeoPoint> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        if (!isEnabled()) {
            log.debug("Google Maps key not configured, skipping geocoding of '{}'", address);
            return Optional.empty();
        }

        String url = properties.getGeocodeUrl()
                + "?address=" + encode(address)
                + "&key=" + encode(properties.getApiKey());

        return call(url, "geocode").flatMap(body -> {
            String status = body.path("status").asText("");
            if (!"OK".equals(status)) {
                log.warn("Geocoding returned status {} for '{}'", status, address);
                return Optional.empty();
            }
            JsonNode first = body.path("results").path(0);
            JsonNode location = first.path("geometry").path("location");
            if (location.isMissingNode() || !location.hasNonNull("lat")) {
                return Optional.empty();
            }
            return Optional.of(new GeoPoint(
                    scale(location.path("lat").asDouble()),
                    scale(location.path("lng").asDouble()),
                    first.path("formatted_address").asText(null)));
        });
    }

    /* ===================== directions ===================== */

    @Override
    public Optional<RouteMetrics> routeMetrics(GeoPoint origin, GeoPoint destination, String waypoints) {
        if (origin == null || destination == null) {
            return Optional.empty();
        }
        if (!isEnabled()) {
            return Optional.empty();
        }

        StringBuilder url = new StringBuilder(properties.getDirectionsUrl())
                .append("?origin=").append(encode(origin.toQueryValue()))
                .append("&destination=").append(encode(destination.toQueryValue()))
                // departure_time=now is what unlocks duration_in_traffic
                .append("&departure_time=now")
                .append("&key=").append(encode(properties.getApiKey()));

        String waypointParam = toWaypointParam(waypoints);
        if (waypointParam != null) {
            url.append("&waypoints=").append(encode(waypointParam));
        }

        return call(url.toString(), "directions").flatMap(body -> {
            String status = body.path("status").asText("");
            if (!"OK".equals(status)) {
                log.warn("Directions returned status {}", status);
                return Optional.empty();
            }

            JsonNode legs = body.path("routes").path(0).path("legs");
            if (!legs.isArray() || legs.isEmpty()) {
                return Optional.empty();
            }

            long metres = 0;
            long seconds = 0;
            long trafficSeconds = 0;
            boolean hasTraffic = false;

            for (JsonNode leg : legs) {
                metres += leg.path("distance").path("value").asLong(0);
                seconds += leg.path("duration").path("value").asLong(0);
                if (leg.path("duration_in_traffic").hasNonNull("value")) {
                    trafficSeconds += leg.path("duration_in_traffic").path("value").asLong(0);
                    hasTraffic = true;
                } else {
                    trafficSeconds += leg.path("duration").path("value").asLong(0);
                }
            }

            if (metres == 0 && seconds == 0) {
                return Optional.empty();
            }

            BigDecimal distanceKm = BigDecimal.valueOf(metres)
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            Integer durationMinutes = toMinutes(seconds);
            Integer trafficMinutes = hasTraffic ? toMinutes(trafficSeconds) : null;

            return Optional.of(new RouteMetrics(
                    distanceKm,
                    durationMinutes,
                    trafficMinutes,
                    GeoUtils.classifyTraffic(durationMinutes, trafficMinutes),
                    RouteMetrics.SOURCE_LIVE_MAPS));
        });
    }

    /* ===================== plumbing ===================== */

    private Optional<JsonNode> call(String url, String label) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Google Maps {} call failed with HTTP {}", label, response.statusCode());
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(response.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Google Maps {} call interrupted", label);
            return Optional.empty();
        } catch (Exception e) {
            // network down, DNS failure, malformed json — never break the caller
            log.warn("Google Maps {} call error: {}", label, e.getMessage());
            return Optional.empty();
        }
    }

    /** "Kurnool;Vellore" -> "Kurnool|Vellore" as the Directions API expects. */
    private String toWaypointParam(String waypoints) {
        if (waypoints == null || waypoints.isBlank()) {
            return null;
        }
        String joined = Arrays.stream(waypoints.split("[;|]"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining("|"));
        return joined.isEmpty() ? null : joined;
    }

    private static Integer toMinutes(long seconds) {
        return (int) Math.max(1, Math.round(seconds / 60.0));
    }

    private static BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
