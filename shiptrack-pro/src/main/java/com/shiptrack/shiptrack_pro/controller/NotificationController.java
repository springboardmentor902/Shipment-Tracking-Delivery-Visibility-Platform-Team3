package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.NotificationPreferenceResponse;
import com.shiptrack.shiptrack_pro.dto.NotificationResponse;
import com.shiptrack.shiptrack_pro.entity.Notification;
import com.shiptrack.shiptrack_pro.entity.NotificationPreference;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.NotificationPreferenceRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.NotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final NotificationPreferenceRepository preferenceRepository;

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
    // Get notifications
    // ==========================================

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            Principal principal) {

        User user = getAuthenticatedUser(principal);

        List<NotificationResponse> response =
                notificationService
                        .getNotificationsForUser(user)
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Get unread count
    // ==========================================

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            Principal principal) {

        User user = getAuthenticatedUser(principal);

        Map<String, Long> response = new HashMap<>();
        response.put(
                "unread",
                notificationService.getUnreadCount(user)
        );

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Mark one notification as read
    // ==========================================

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long notificationId,
            Principal principal) {

        User user = getAuthenticatedUser(principal);

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
    // Mark all notifications as read
    // ==========================================

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            Principal principal) {

        User user = getAuthenticatedUser(principal);

        int updated =
                notificationService.markAllAsRead(user);

        Map<String, Integer> response = new HashMap<>();
        response.put("updated", updated);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Get notification preferences
    // ==========================================

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse>
    getPreferences(Principal principal) {

        User user = getAuthenticatedUser(principal);

        NotificationPreference preference =
                getOrCreatePreferences(user);

        return ResponseEntity.ok(
                mapPreferenceToResponse(preference, user)
        );
    }

    // ==========================================
    // Update notification preferences
    // ==========================================

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse>
    updatePreferences(
            @RequestBody Map<String, Object> patch,
            Principal principal) {

        User user = getAuthenticatedUser(principal);

        NotificationPreference preference =
                getOrCreatePreferences(user);

        if (patch.containsKey("inAppEnabled")) {
            preference.setInAppEnabled(
                    Boolean.TRUE.equals(
                            patch.get("inAppEnabled")
                    )
            );
        }

        if (patch.containsKey("emailEnabled")) {
            preference.setEmailEnabled(
                    Boolean.TRUE.equals(
                            patch.get("emailEnabled")
                    )
            );
        }

        if (patch.containsKey("smsEnabled")) {
            preference.setSmsEnabled(
                    Boolean.TRUE.equals(
                            patch.get("smsEnabled")
                    )
            );
        }

        if (patch.containsKey("notifyStatusChange")) {
            preference.setNotifyStatusChange(
                    Boolean.TRUE.equals(
                            patch.get("notifyStatusChange")
                    )
            );
        }

        if (patch.containsKey("notifyDelayRisk")) {
            preference.setNotifyDelayRisk(
                    Boolean.TRUE.equals(
                            patch.get("notifyDelayRisk")
                    )
            );
        }

        if (patch.containsKey("notifyDelivery")) {
            preference.setNotifyDelivery(
                    Boolean.TRUE.equals(
                            patch.get("notifyDelivery")
                    )
            );
        }

        if (patch.containsKey("minRiskLevel")) {
            String riskLevel =
                    String.valueOf(
                            patch.get("minRiskLevel")
                    ).toUpperCase();

            if (riskLevel.equals("LOW")
                    || riskLevel.equals("MEDIUM")
                    || riskLevel.equals("HIGH")
                    || riskLevel.equals("CRITICAL")) {

                preference.setMinRiskLevel(riskLevel);
            }
        }

        preference =
                preferenceRepository.save(preference);

        return ResponseEntity.ok(
                mapPreferenceToResponse(
                        preference,
                        user
                )
        );
    }

    // ==========================================
    // Authenticated user
    // ==========================================

    private User getAuthenticatedUser(
            Principal principal) {

        if (principal == null) {
            throw new RuntimeException(
                    "Authenticated user is required"
            );
        }

        String email = principal.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found: "
                                        + email
                        ));
    }

    // ==========================================
    // Get or create preferences
    // ==========================================

    private NotificationPreference getOrCreatePreferences(
            User user) {

        return preferenceRepository
                .findByUser(user)
                .orElseGet(() ->
                        preferenceRepository.save(
                                NotificationPreference.builder()
                                        .user(user)
                                        .inAppEnabled(true)
                                        .emailEnabled(true)
                                        .smsEnabled(false)
                                        .notifyStatusChange(true)
                                        .notifyDelayRisk(true)
                                        .notifyDelivery(true)
                                        .minRiskLevel("HIGH")
                                        .build()
                        )
                );
    }

    // ==========================================
    // Preference -> Response
    // ==========================================

    private NotificationPreferenceResponse
    mapPreferenceToResponse(
            NotificationPreference preference,
            User user) {

        return NotificationPreferenceResponse.builder()
                .inAppEnabled(preference.isInAppEnabled())
                .emailEnabled(preference.isEmailEnabled())
                .smsEnabled(preference.isSmsEnabled())
                .notifyStatusChange(
                        preference.isNotifyStatusChange()
                )
                .notifyDelayRisk(
                        preference.isNotifyDelayRisk()
                )
                .notifyDelivery(
                        preference.isNotifyDelivery()
                )
                .minRiskLevel(
                        preference.getMinRiskLevel()
                )
                .emailChannelAvailable(
                        user.getEmail() != null
                                && !user.getEmail().isBlank()
                )
                .smsChannelAvailable(
                        user.getPhone() != null
                                && !user.getPhone().isBlank()
                )
                .phone(user.getPhone())
                .build();
    }

    // ==========================================
    // Notification -> Response DTO
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
                                ? notification.getShipment()
                                        .getTrackingNumber()
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