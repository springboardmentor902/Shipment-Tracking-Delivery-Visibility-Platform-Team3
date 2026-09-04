package com.shiptrack.shiptrack_pro.service;

import java.net.URI;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OSRMTableService {

    private final RestClient restClient;

    public OSRMTableService() {
        this.restClient = RestClient.create();
    }

    public Map<String, Object> getDistanceMatrix(
            double originLon,
            double originLat,
            double destinationLon,
            double destinationLat) {

        String coordinates =
                originLon + "," + originLat + ";" +
                destinationLon + "," + destinationLat;

        URI uri = UriComponentsBuilder
                .fromUriString(
                        "https://router.project-osrm.org/table/v1/driving/"
                                + coordinates)
                .queryParam("annotations", "distance,duration")
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