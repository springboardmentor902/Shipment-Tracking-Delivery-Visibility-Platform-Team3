package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ETAScheduledService {

    private final ETAPredictionService etaPredictionService;
    private final ShipmentRepository shipmentRepository;

    @Scheduled(fixedRate = 900000)
    public void recalculateETAs() {

        // Find all in-progress shipments
        List<Shipment> shipments =
                shipmentRepository.findByStatus("IN_PROGRESS");

        // Recalculate ETA for each shipment
        for (Shipment shipment : shipments) {

            etaPredictionService.recalculateETA(
                    shipment.getId()
            );
        }
    }
}