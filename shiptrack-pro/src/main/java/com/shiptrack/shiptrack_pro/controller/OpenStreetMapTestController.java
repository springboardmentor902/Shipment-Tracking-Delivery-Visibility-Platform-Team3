package com.shiptrack.shiptrack_pro.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shiptrack.shiptrack_pro.service.OpenStreetMapService;

@RestController
@RequestMapping("/api/openstreetmap")
public class OpenStreetMapTestController {

    private final OpenStreetMapService openStreetMapService;

    public OpenStreetMapTestController(OpenStreetMapService openStreetMapService) {
        this.openStreetMapService = openStreetMapService;
    }

    @GetMapping("/geocode")
    public List<Map<String, Object>> geocode(
            @RequestParam String address) {

        return openStreetMapService.geocodeAddress(address);
    }
}