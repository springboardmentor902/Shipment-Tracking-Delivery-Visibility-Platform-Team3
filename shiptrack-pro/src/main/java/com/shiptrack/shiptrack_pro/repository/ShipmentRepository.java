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
import java.util.Collection;
import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    /* ---- scoped listings: a user only ever sees shipments they are tied to ---- */

    Page<Shipment> findByCreatedBy(User createdBy, Pageable pageable);

    Page<Shipment> findByCreatedByAndStatus(User createdBy, ShipmentStatus status, Pageable pageable);

    Page<Shipment> findByAssignedOperator(User assignedOperator, Pageable pageable);

    Page<Shipment> findByAssignedOperatorAndStatus(User assignedOperator, ShipmentStatus status, Pageable pageable);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);

    List<Shipment> findByStatusIn(Collection<ShipmentStatus> statuses);

    // customer is the receiver, not the creator, so match on email
    Page<Shipment> findByReceiverEmailIgnoreCase(String receiverEmail, Pageable pageable);

    Page<Shipment> findByReceiverEmailIgnoreCaseAndStatus(String receiverEmail,
                                                          ShipmentStatus status,
                                                          Pageable pageable);

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

    /**
     * A customer sees shipments they booked themselves plus shipments addressed
     * to them as the receiver. Nothing else is ever visible.
     */
    @Query("""
           SELECT s FROM Shipment s
           WHERE s.createdBy = :user
              OR LOWER(s.receiverEmail) = LOWER(:email)
           """)
    Page<Shipment> findVisibleToCustomer(@Param("user") User user,
                                         @Param("email") String email,
                                         Pageable pageable);

    @Query("""
           SELECT s FROM Shipment s
           WHERE (s.createdBy = :user OR LOWER(s.receiverEmail) = LOWER(:email))
             AND s.status = :status
           """)
    Page<Shipment> findVisibleToCustomerByStatus(@Param("user") User user,
                                                 @Param("email") String email,
                                                 @Param("status") ShipmentStatus status,
                                                 Pageable pageable);

    /**
     * A business client sees shipments created under their business account,
     * whether created by themselves or by a customer account linked to the
     * same business id (linked-customer behaviour, MM-20).
     */
    @Query("""
           SELECT s FROM Shipment s
           WHERE s.createdBy = :user
              OR (:businessId IS NOT NULL AND s.businessId = :businessId)
           """)
    Page<Shipment> findVisibleToBusiness(@Param("user") User user,
                                         @Param("businessId") Long businessId,
                                         Pageable pageable);

    @Query("""
           SELECT s FROM Shipment s
           WHERE (s.createdBy = :user OR (:businessId IS NOT NULL AND s.businessId = :businessId))
             AND s.status = :status
           """)
    Page<Shipment> findVisibleToBusinessByStatus(@Param("user") User user,
                                                 @Param("businessId") Long businessId,
                                                 @Param("status") ShipmentStatus status,
                                                 Pageable pageable);
}
