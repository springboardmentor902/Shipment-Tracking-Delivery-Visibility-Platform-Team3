package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    /* ---- scoped listings: a user only ever sees shipments they are tied to ---- */

    Page<Shipment> findByCreatedBy(User createdBy, Pageable pageable);

    Page<Shipment> findByCreatedByAndStatus(User createdBy, ShipmentStatus status, Pageable pageable);

    Page<Shipment> findByAssignedOperator(User assignedOperator, Pageable pageable);

    Page<Shipment> findByAssignedOperatorAndStatus(User assignedOperator, ShipmentStatus status, Pageable pageable);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);

    /** Operators see shipments assigned to them plus any they created themselves. */
    @Query("""
           SELECT s FROM Shipment s
           WHERE s.assignedOperator = :user OR s.createdBy = :user
           """)
    Page<Shipment> findVisibleToOperator(@Param("user") User user, Pageable pageable);

    @Query("""
           SELECT s FROM Shipment s
           WHERE (s.assignedOperator = :user OR s.createdBy = :user)
             AND s.status = :status
           """)
    Page<Shipment> findVisibleToOperatorByStatus(@Param("user") User user,
                                                 @Param("status") ShipmentStatus status,
                                                 Pageable pageable);
}