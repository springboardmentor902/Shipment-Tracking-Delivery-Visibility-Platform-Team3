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

    public GpsLocationController(
            GpsLocationService gpsLocationService) {

        this.gpsLocationService = gpsLocationService;
    }

    @PostMapping("/location")
    public ResponseEntity<GpsLocation> saveLocation(
            @RequestParam Long shipmentId,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        return ResponseEntity.ok(
                gpsLocationService.saveLocation(
                        shipmentId,
                        latitude,
                        longitude
                )
        );
    }

    @GetMapping("/location/{shipmentId}")
    public ResponseEntity<List<GpsLocation>> getShipmentLocations(
            @PathVariable Long shipmentId) {

        return ResponseEntity.ok(
                gpsLocationService.getVehicleLocations(shipmentId)
        );
    }
}