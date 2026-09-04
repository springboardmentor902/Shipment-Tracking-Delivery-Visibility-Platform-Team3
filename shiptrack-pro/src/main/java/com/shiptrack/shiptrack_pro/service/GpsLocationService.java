package com.shiptrack.shiptrack_pro.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shiptrack.shiptrack_pro.entity.GpsLocation;
import com.shiptrack.shiptrack_pro.repository.GpsLocationRepository;

@Service
public class GpsLocationService {

    private final GpsLocationRepository gpsLocationRepository;

    public GpsLocationService(GpsLocationRepository gpsLocationRepository) {
        this.gpsLocationRepository = gpsLocationRepository;
    }

    public GpsLocation saveLocation(
            Long vehicleId,
            double latitude,
            double longitude) {

        GpsLocation location = new GpsLocation();

        location.setVehicleId(vehicleId);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setRecordedAt(LocalDateTime.now());

        return gpsLocationRepository.save(location);
    }

    public List<GpsLocation> getVehicleLocations(Long vehicleId) {
        return gpsLocationRepository
                .findByVehicleIdOrderByRecordedAtDesc(vehicleId);
    }
}