package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.NotificationResponse;
import com.shiptrack.shiptrack_pro.entity.Notification;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.NotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;

    // ==========================================
    // Send notification
    // ==========================================

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(
            @RequestParam Long userId,
            @RequestParam Long shipmentId,
            @RequestParam String type) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId
                        ));

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Shipment not found with id: " + shipmentId
                        ));

        Notification notification =
                notificationService.send(
                        type,
                        user,
                        shipment
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapToResponse(notification));
    }

    // ==========================================
    // Get notifications for user
    // ==========================================

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @PathVariable Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId
                        ));

        List<NotificationResponse> response =
                notificationService
                        .getNotificationsForUser(user)
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Mark notification as read
    // ==========================================

    @PutMapping("/{notificationId}/read/{userId}")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long notificationId,
            @PathVariable Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + userId
                        ));

        Notification notification =
                notificationService.markAsRead(
                        notificationId,
                        user
                );

        return ResponseEntity.ok(
                mapToResponse(notification)
        );
    }

    // ==========================================
    // Entity -> Response DTO
    // ==========================================

    private NotificationResponse mapToResponse(
            Notification notification) {

        return NotificationResponse.builder()
                .id(notification.getId())

                .userId(
                        notification.getUser() != null
                                ? notification.getUser().getId()
                                : null
                )

                .shipmentId(
                        notification.getShipment() != null
                                ? notification.getShipment().getId()
                                : null
                )

                .trackingNumber(
                        notification.getShipment() != null
                                ? notification.getShipment().getTrackingNumber()
                                : null
                )

                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())

                .status(notification.getStatus())

                .deliveryStatus(
                        notification.getDeliveryStatus()
                )

                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())

                .build();
    }
}