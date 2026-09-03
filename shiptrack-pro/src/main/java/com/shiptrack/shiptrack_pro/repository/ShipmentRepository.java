package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    boolean existsByTrackingNumber(String trackingNumber);

    List<Shipment> findByCustomer(User customer);
    List<Shipment> findByStatus(String status);
    Optional<Shipment> findByTrackingNumberAndCustomer(
            String trackingNumber,
            User customer
    );
}