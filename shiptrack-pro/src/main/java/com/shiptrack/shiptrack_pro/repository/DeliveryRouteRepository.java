package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.DeliveryRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, Long> {

    Optional<DeliveryRoute> findByShipmentId(Long shipmentId);
}
