package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findAllByShipmentIdOrderByCreatedAtDesc(
            Long shipmentId
    );

    List<Route> findAllByShipmentIdOrderByCreatedAtAsc(
            Long shipmentId
    );

    Optional<Route> findByShipmentIdAndIsCurrentTrue(
            Long shipmentId
    );
}