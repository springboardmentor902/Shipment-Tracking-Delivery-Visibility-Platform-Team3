package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.entity.Notification;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.NotificationRepository;
import com.shiptrack.shiptrack_pro.service.NotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    // ==========================================
    // Send Notification
    // ==========================================

    @Override
    public Notification send(
            String type,
            User user,
            Shipment shipment) {

        // Prevent duplicate notifications
        return notificationRepository
                .findByUserAndShipmentAndType(
                        user,
                        shipment,
                        type
                )
                .orElseGet(() -> {

                    Notification notification =
                            Notification.builder()
                                    .user(user)
                                    .shipment(shipment)
                                    .type(type)
                                    .title("Shipment Update")
                                    .message(
                                            "There is an update for shipment "
                                                    + shipment.getTrackingNumber()
                                    )
                                    .status("UNREAD")
                                    .deliveryStatus("SENT")
                                    .sentAt(LocalDateTime.now())
                                    .createdAt(LocalDateTime.now())
                                    .build();

                    return notificationRepository.save(notification);
                });
    }

    // ==========================================
    // Get User Notifications
    // ==========================================

    @Override
    public List<Notification> getNotificationsForUser(
            User user) {

        return notificationRepository
                .findByUserOrderByCreatedAtDesc(user);
    }

    // ==========================================
    // Mark Notification as Read
    // ==========================================

    @Override
    public Notification markAsRead(
            Long notificationId,
            User user) {

        Notification notification =
                notificationRepository
                        .findByIdAndUser(
                                notificationId,
                                user
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification not found"
                                ));

        notification.setStatus("READ");
        notification.setReadAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }
}