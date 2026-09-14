package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.ETAPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ETAPredictionRepository extends JpaRepository<ETAPrediction, Long> {

    List<ETAPrediction> findByShipmentIdOrderByCalculatedAtDesc(Long shipmentId);
}