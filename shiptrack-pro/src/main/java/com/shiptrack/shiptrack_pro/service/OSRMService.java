package com.shiptrack.shiptrack_pro.service;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OSRMService {

    private final RestClient restClient;

    public OSRMService() {
        this.restClient = RestClient.create();
    }

    // =========================================================
    // GEOCODE ADDRESS USING OPENSTREETMAP NOMINATIM
    // =========================================================

    public Map<String, Object> geocodeAddress(String address) {

        URI uri = UriComponentsBuilder
                .fromUriString(
                        "https://nominatim.openstreetmap.org/search")
                .queryParam("q", address)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .build()
                .encode()
                .toUri();

        List<Map<String, Object>> results =
                restClient.get()
                        .uri(uri)
                        .header(
                                "User-Agent",
                                "ShipTrack-Pro/1.0"
                        )
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(List.class);

        if (results == null || results.isEmpty()) {
            throw new RuntimeException(
                    "Location not found for address: " + address
            );
        }

        return results.get(0);
    }

    // =========================================================
    // GET NORMAL ROUTE
    // =========================================================

    public Map<String, Object> getRoute(
            double originLon,
            double originLat,
            double destinationLon,
            double destinationLat) {

        String coordinates =
                originLon + "," + originLat + ";" +
                destinationLon + "," + destinationLat;

        URI uri = UriComponentsBuilder
                .fromUriString(
                        "https://router.project-osrm.org/route/v1/driving/"
                                + coordinates)
                .queryParam("overview", "false")
                .queryParam("steps", "true")
                .build()
                .encode()
                .toUri();

        return restClient.get()
                .uri(uri)
                .header("Accept-Encoding", "identity")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);
    }

    // =========================================================
    // GET ALTERNATIVE ROUTES
    // =========================================================

    public Map<String, Object> getAlternativeRoutes(
            double originLon,
            double originLat,
            double destinationLon,
            double destinationLat) {

        String coordinates =
                originLon + "," + originLat + ";" +
                destinationLon + "," + destinationLat;

        URI uri = UriComponentsBuilder
                .fromUriString(
                        "https://router.project-osrm.org/route/v1/driving/"
                                + coordinates)
                .queryParam("overview", "false")
                .queryParam("steps", "true")
                .queryParam("alternatives", "true")
                .build()
                .encode()
                .toUri();

        return restClient.get()
                .uri(uri)
                .header("Accept-Encoding", "identity")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);
    }
}