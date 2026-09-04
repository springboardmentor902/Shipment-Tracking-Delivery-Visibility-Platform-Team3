package com.shiptrack.shiptrack_pro.service;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleMapsService {

    private final RestClient restClient;

    @Value("${google.maps.api-key}")
    private String apiKey;

    @Value("${google.maps.geocoding-url}")
    private String geocodingUrl;

    @Value("${google.maps.directions-url}")
    private String directionsUrl;

    @Value("${google.maps.distance-matrix-url}")
    private String distanceMatrixUrl;

    public GoogleMapsService() {
        this.restClient = RestClient.create();
    }

    /**
     * Convert an address into latitude and longitude.
     */
    public Map<String, Object> geocodeAddress(String address) {

        URI uri = UriComponentsBuilder
                .fromUriString(geocodingUrl)
                .queryParam("address", address)
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();

        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);
    }

    /**
     * Get route information between two locations.
     */
    public Map<String, Object> getDirections(String origin, String destination) {

        URI uri = UriComponentsBuilder
                .fromUriString(directionsUrl)
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();

        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);
    }

    /**
     * Get distance and estimated travel time between two locations.
     */
    public Map<String, Object> getDistanceMatrix(String origin, String destination) {

        URI uri = UriComponentsBuilder
                .fromUriString(distanceMatrixUrl)
                .queryParam("origins", origin)
                .queryParam("destinations", destination)
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();

        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);
    }
}