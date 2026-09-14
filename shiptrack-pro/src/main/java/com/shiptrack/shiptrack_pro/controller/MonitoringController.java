package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class MonitoringController {

	private final ShipmentRepository shipmentRepository;

	@GetMapping("/active")
	public ResponseEntity<List<Shipment>> getActiveShipments() {

		List<Shipment> activeShipments = shipmentRepository.findAll().stream()
				.filter(shipment -> shipment.getStatus() != null)
				.filter(shipment -> !isCompleted(shipment.getStatus().toString())).toList();

		return ResponseEntity.ok(activeShipments);
	}

	private boolean isCompleted(String status) {

		String normalizedStatus = status.trim().toUpperCase();

		return normalizedStatus.equals("DELIVERED") || normalizedStatus.equals("COMPLETED")
				|| normalizedStatus.equals("CANCELLED");
	}
}