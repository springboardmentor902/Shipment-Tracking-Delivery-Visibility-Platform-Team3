package com.shiptrack.shiptrack_pro.scheduler;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.service.ETAPredictionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ETAAutoRecalculationScheduler {

    private final ShipmentRepository shipmentRepository;
    private final ETAPredictionService etaPredictionService;

    @Scheduled(fixedRate = 300000)
    public void recalculateActiveShipments() {

        List<Shipment> shipments =
                shipmentRepository.findByStatus("IN_TRANSIT");

        log.info(
                "Starting automatic ETA recalculation for {} shipments",
                shipments.size()
        );

        for (Shipment shipment : shipments) {

            try {

                etaPredictionService.recalculateETA(
                        shipment.getId()
                );

                log.info(
                        "ETA recalculated successfully for shipment {}",
                        shipment.getId()
                );

            } catch (Exception e) {

                log.error(
                        "Could not recalculate ETA for shipment {}",
                        shipment.getId(),
                        e
                );
            }
        }
    }
}