package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.DeliveryRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, Long> {

    /** All legs of a shipment, in travel order. */
    List<DeliveryRoute> findByShipmentIdOrderByLegNumberAsc(Long shipmentId);

    Optional<DeliveryRoute> findByShipmentIdAndLegNumber(Long shipmentId, Integer legNumber);

    boolean existsByShipmentIdAndLegNumber(Long shipmentId, Integer legNumber);

    /** Work queue of a driver, newest legs last. */
    List<DeliveryRoute> findByDriverIdOrderByIdAsc(Long driverId);

    @Query("SELECT COALESCE(MAX(r.legNumber), 0) FROM DeliveryRoute r WHERE r.shipment.id = :shipmentId")
    Integer findMaxLegNumber(@Param("shipmentId") Long shipmentId);
}
