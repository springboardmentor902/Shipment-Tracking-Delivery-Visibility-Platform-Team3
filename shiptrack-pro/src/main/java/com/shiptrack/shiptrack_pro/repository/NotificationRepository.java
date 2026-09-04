package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.Notification;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(
            User user
    );

    Optional<Notification> findByIdAndUser(
            Long id,
            User user
    );

    Optional<Notification> findByUserAndShipmentAndType(
            User user,
            Shipment shipment,
            String type
    );
}