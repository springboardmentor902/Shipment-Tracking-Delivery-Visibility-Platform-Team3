package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.ETAPrediction;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ETAPredictionRepository extends JpaRepository<ETAPrediction, Long> {

    Optional<ETAPrediction> findByShipment(Shipment shipment);

    Optional<ETAPrediction> findByShipmentId(Long shipmentId);
}