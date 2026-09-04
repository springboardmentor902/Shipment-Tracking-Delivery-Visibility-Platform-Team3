package com.shiptrack.shiptrack_pro.service;

import java.net.URI;
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
}