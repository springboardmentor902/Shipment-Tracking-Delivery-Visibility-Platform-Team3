package com.shiptrack.shiptrack_pro.service;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OpenStreetMapService {

    private final RestClient restClient;

    @Value("${osm.nominatim.url}")
    private String nominatimUrl;

    @Value("${osm.user-agent}")
    private String userAgent;

    public OpenStreetMapService() {
        this.restClient = RestClient.create();
    }

    /**
     * Convert a text address into latitude and longitude
     * using the OpenStreetMap Nominatim API.
     */
    public List<Map<String, Object>> geocodeAddress(String address) {

        URI uri = UriComponentsBuilder
                .fromUriString(nominatimUrl + "/search")
                .queryParam("q", address)
                .queryParam("format", "json")
                .queryParam("limit", 5)
                .build()
                .encode()
                .toUri();

        return restClient.get()
                .uri(uri)
                .header("User-Agent", userAgent)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(List.class);
    }
}