package com.shiptrack.shiptrack_pro.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shiptrack.shiptrack_pro.service.OSRMTableService;

@RestController
@RequestMapping("/api/osrm")
public class OSRMTableTestController {

    private final OSRMTableService osrmTableService;

    public OSRMTableTestController(OSRMTableService osrmTableService) {
        this.osrmTableService = osrmTableService;
    }

    @GetMapping("/distance-matrix")
    public Map<String, Object> getDistanceMatrix(
            @RequestParam double originLon,
            @RequestParam double originLat,
            @RequestParam double destinationLon,
            @RequestParam double destinationLat) {

        return osrmTableService.getDistanceMatrix(
                originLon,
                originLat,
                destinationLon,
                destinationLat);
    }
}