package com.shiptrack.shiptrack_pro.service;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OpenStreetMapService {

    private final RestClient restClient;

    @Value("${osm.nominatim.url:https://nominatim.openstreetmap.org}")
    private String nominatimUrl;

    @Value("${osm.user-agent:ShipTrack-Pro/1.0}")
    private String userAgent;

    public OpenStreetMapService() {
        this.restClient = RestClient.builder().build();
    }

    public List<Map<String, Object>> geocodeAddress(String address) {

        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be empty");
        }

        URI uri = UriComponentsBuilder
                .fromUriString(nominatimUrl)
                .path("/search")
                .queryParam("q", address.trim())
                .queryParam("format", "json")
                .queryParam("limit", 5)
                .build()
                .encode()
                .toUri();

        try {
            System.out.println("Nominatim request: " + uri);

            List<Map<String, Object>> results = restClient.get()
                    .uri(uri)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            System.out.println(
                    "Nominatim results: "
                            + (results == null ? 0 : results.size())
            );

            return results != null ? results : Collections.emptyList();

        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException(
                    "Nominatim geocoding failed for address: "
                            + address
                            + " | "
                            + e.getMessage(),
                    e
            );
        }
    }
}