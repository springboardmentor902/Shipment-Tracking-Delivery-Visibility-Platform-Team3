package com.shiptrack.shiptrack_pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiptrack.shiptrack_pro.entity.GpsLocation;

public interface GpsLocationRepository extends JpaRepository<GpsLocation, Long> {

    List<GpsLocation> findByVehicleIdOrderByRecordedAtDesc(Long vehicleId);
}