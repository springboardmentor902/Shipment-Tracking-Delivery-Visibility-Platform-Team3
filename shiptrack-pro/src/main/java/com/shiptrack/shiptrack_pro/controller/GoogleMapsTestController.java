package com.shiptrack.shiptrack_pro.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shiptrack.shiptrack_pro.service.GoogleMapsService;

@RestController
@RequestMapping("/api/google-maps")
public class GoogleMapsTestController {

    private final GoogleMapsService googleMapsService;

    public GoogleMapsTestController(GoogleMapsService googleMapsService) {
        this.googleMapsService = googleMapsService;
    }

    @GetMapping("/geocode")
    public Map<String, Object> geocode(
            @RequestParam String address) {

        return googleMapsService.geocodeAddress(address);
    }
}