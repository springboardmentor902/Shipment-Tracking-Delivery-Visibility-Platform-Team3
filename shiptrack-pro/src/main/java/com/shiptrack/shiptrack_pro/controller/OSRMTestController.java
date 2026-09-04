package com.shiptrack.shiptrack_pro.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shiptrack.shiptrack_pro.service.OSRMService;

@RestController
@RequestMapping("/api/osrm")
public class OSRMTestController {

    private final OSRMService osrmService;

    public OSRMTestController(OSRMService osrmService) {
        this.osrmService = osrmService;
    }

    @GetMapping("/route")
    public Map<String, Object> getRoute(
            @RequestParam double originLon,
            @RequestParam double originLat,
            @RequestParam double destinationLon,
            @RequestParam double destinationLat) {

        return osrmService.getRoute(
                originLon,
                originLat,
                destinationLon,
                destinationLat);
    }
}