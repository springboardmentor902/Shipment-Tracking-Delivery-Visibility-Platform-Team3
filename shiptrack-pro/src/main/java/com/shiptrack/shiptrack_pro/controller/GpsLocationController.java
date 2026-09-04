package com.shiptrack.shiptrack_pro.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.shiptrack.shiptrack_pro.entity.GpsLocation;
import com.shiptrack.shiptrack_pro.service.GpsLocationService;

@RestController
@RequestMapping("/api/gps")
public class GpsLocationController {

    private final GpsLocationService gpsLocationService;

    public GpsLocationController(GpsLocationService gpsLocationService) {
        this.gpsLocationService = gpsLocationService;
    }

    @PostMapping("/location")
    public ResponseEntity<GpsLocation> saveLocation(
            @RequestParam Long vehicleId,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        return ResponseEntity.ok(
                gpsLocationService.saveLocation(
                        vehicleId, latitude, longitude));
    }

    @GetMapping("/location/{vehicleId}")
    public ResponseEntity<List<GpsLocation>> getVehicleLocations(
            @PathVariable Long vehicleId) {

        return ResponseEntity.ok(
                gpsLocationService.getVehicleLocations(vehicleId));
    }
}