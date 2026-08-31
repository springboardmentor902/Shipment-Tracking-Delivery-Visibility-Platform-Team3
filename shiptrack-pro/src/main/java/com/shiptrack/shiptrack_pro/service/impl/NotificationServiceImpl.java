package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.NotificationPreferenceRequest;
import com.shiptrack.shiptrack_pro.dto.NotificationPreferenceResponse;
import com.shiptrack.shiptrack_pro.dto.NotificationResponse;
import com.shiptrack.shiptrack_pro.entity.*;
import com.shiptrack.shiptrack_pro.repository.NotificationPreferenceRepository;
import com.shiptrack.shiptrack_pro.repository.NotificationRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.service.AlertSender;
import com.shiptrack.shiptrack_pro.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Raises alerts, stores them, and serves each user their own inbox.
 *
 * Three rules shape this class:
 *
 * 1. The in-app row is the record; email and SMS are best-effort attempts on top,
 *    each stamped so a failed send is visible instead of silent.
 * 2. Nothing here may break the delivery event that triggered it, so every
 *    raise path is wrapped and only logged on failure.
 * 3. A repeated event inside the cooldown window is dropped, because a shipment
 *    whose ETA recalculates on every location ping would otherwise send the same
 *    "at risk" alert dozens of times.
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");
    private static final int MAX_LIST_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AlertSender emailSender;
    private final AlertSender smsSender;
    private final int cooldownMinutes;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   NotificationPreferenceRepository preferenceRepository,
                                   UserRepository userRepository,
                                   CurrentUserService currentUserService,
                                   @Qualifier("emailAlertSender") AlertSender emailSender,
                                   @Qualifier("smsAlertSender") AlertSender smsSender,
                                   @Value("${notifications.cooldown-minutes:30}") int cooldownMinutes) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.cooldownMinutes = cooldownMinutes;
    }

    /* ===================== raising alerts ===================== */

    @Override
    @Transactional
    public void notifyStatusChange(Shipment shipment, String note, String actorName) {
        try {
            ShipmentStatus status = shipment.getStatus();
            boolean delivered = status == ShipmentStatus.DELIVERED;
            boolean cancelled = status == ShipmentStatus.CANCELLED;

            NotificationType type = delivered ? NotificationType.DELIVERED
                    : cancelled ? NotificationType.CANCELLED
                    : NotificationType.STATUS_CHANGE;

            String readableStatus = readable(status);
            String title = delivered
                    ? "Delivered: " + shipment.getTrackingNumber()
                    : shipment.getTrackingNumber() + " is now " + readableStatus;

            StringBuilder message = new StringBuilder();
            message.append("Shipment ").append(shipment.getTrackingNumber())
                    .append(" to ").append(nullSafe(shipment.getReceiverName()))
                    .append(" is now ").append(readableStatus).append('.');
            if (note != null && !note.isBlank()) {
                message.append(' ').append(note.trim());
            }
            if (actorName != null && !actorName.isBlank()) {
                message.append(" Updated by ").append(actorName).append('.');
            }

            dispatch(shipment, type,
                    cancelled ? NotificationSeverity.WARNING : NotificationSeverity.INFO,
                    title, message.toString(),
                    "STATUS:" + shipment.getId() + ":" + status.name(),
                    recipientsFor(shipment, false));
        } catch (RuntimeException ex) {
            log.warn("Could not raise the status alert for shipment {}: {}",
                    shipment.getId(), ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void notifyDelayRisk(Shipment shipment, DelayRiskLevel level, int riskScore,
                                LocalDateTime predictedDeliveryAt, Integer expectedDelayMinutes) {
        try {
            String title = (level == DelayRiskLevel.CRITICAL ? "Delivery at serious risk: " : "Delivery at risk: ")
                    + shipment.getTrackingNumber();

            StringBuilder message = new StringBuilder();
            message.append("Shipment ").append(shipment.getTrackingNumber())
                    .append(" has a delay risk of ").append(riskScore).append("/100 (")
                    .append(level.name().toLowerCase()).append(").");
            if (predictedDeliveryAt != null) {
                message.append(" Now expected ").append(WHEN.format(predictedDeliveryAt)).append('.');
            }
            if (expectedDelayMinutes != null && expectedDelayMinutes > 0) {
                message.append(" That is about ").append(humaniseDelay(expectedDelayMinutes))
                        .append(" past the promised date.");
            }

            dispatch(shipment, NotificationType.DELAY_RISK,
                    level == DelayRiskLevel.CRITICAL ? NotificationSeverity.CRITICAL
                            : NotificationSeverity.WARNING,
                    title, message.toString(),
                    // one alert per shipment per risk level per cooldown window
                    "DELAY:" + shipment.getId() + ":" + level.name(),
                    // the operator carrying the shipment needs to know too
                    recipientsFor(shipment, true));
        } catch (RuntimeException ex) {
            log.warn("Could not raise the delay alert for shipment {}: {}",
                    shipment.getId(), ex.getMessage());
        }
    }

    /**
     * Writes one in-app row per interested user and attempts the extra channels
     * each of them has asked for.
     */
    private void dispatch(Shipment shipment, NotificationType type, NotificationSeverity severity,
                          String title, String message, String dedupeKey, List<Recipient> recipients) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(Math.max(0, cooldownMinutes));

        for (Recipient recipient : recipients) {
            User user = recipient.user();

            NotificationPreference preferences = user != null ? preferencesFor(user) : null;
            if (preferences != null && !wants(preferences, type, severity)) {
                continue;
            }

            if (user != null && dedupeKey != null
                    && notificationRepository.existsByRecipientIdAndDedupeKeyAndCreatedAtAfter(
                            user.getId(), dedupeKey, since)) {
                // already told this person about this exact event recently
                continue;
            }

            boolean wantsEmail = preferences == null || preferences.isEmailEnabled();
            boolean wantsSms = preferences != null && preferences.isSmsEnabled();
            // SMS is reserved for alerts worth interrupting someone over
            boolean smsWorthy = severity != NotificationSeverity.INFO
                    || type == NotificationType.DELIVERED;

            LocalDateTime emailSentAt = null;
            LocalDateTime smsSentAt = null;

            if (wantsEmail && recipient.email() != null
                    && emailSender.send(recipient.email(), title, message + signature(shipment))) {
                emailSentAt = LocalDateTime.now();
            }
            if (wantsSms && smsWorthy && recipient.phone() != null
                    && smsSender.send(recipient.phone(), title, message)) {
                smsSentAt = LocalDateTime.now();
            }

            if (user == null) {
                // a receiver with no account: contacted directly, nothing to store
                continue;
            }
            if (preferences != null && !preferences.isInAppEnabled()) {
                continue;
            }

            notificationRepository.save(Notification.builder()
                    .recipient(user)
                    .shipment(shipment)
                    .type(type)
                    .severity(severity)
                    .title(trim(title, 200))
                    .message(trim(message, 1000))
                    .dedupeKey(dedupeKey)
                    .read(false)
                    .emailSentAt(emailSentAt)
                    .smsSentAt(smsSentAt)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Everyone with a stake in this shipment: whoever booked it, the receiver,
     * and for delay alerts the operator who has to act.
     *
     * A receiver without an account is still contacted by email or SMS, because
     * the person waiting at the door is the one who most needs to know.
     */
    private List<Recipient> recipientsFor(Shipment shipment, boolean includeOperator) {
        Map<String, Recipient> unique = new LinkedHashMap<>();

        addUser(unique, shipment.getCreatedBy());

        User receiverAccount = shipment.getReceiverEmail() == null ? null
                : userRepository.findByEmail(shipment.getReceiverEmail()).orElse(null);
        if (receiverAccount != null) {
            addUser(unique, receiverAccount);
        } else if (notBlank(shipment.getReceiverEmail()) || notBlank(shipment.getReceiverPhone())) {
            Recipient contact = new Recipient(null, blankToNull(shipment.getReceiverEmail()),
                    blankToNull(shipment.getReceiverPhone()));
            unique.putIfAbsent("contact:" + shipment.getReceiverEmail() + shipment.getReceiverPhone(), contact);
        }

        if (includeOperator) {
            addUser(unique, shipment.getAssignedOperator());
        }

        return new ArrayList<>(unique.values());
    }

    private void addUser(Map<String, Recipient> target, User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        target.putIfAbsent("user:" + user.getId(),
                new Recipient(user, blankToNull(user.getEmail()), blankToNull(user.getPhone())));
    }

    /** Does this user want to hear about this kind of event at this severity? */
    private boolean wants(NotificationPreference preferences, NotificationType type,
                          NotificationSeverity severity) {
        return switch (type) {
            case STATUS_CHANGE, CANCELLED -> preferences.isNotifyStatusChange();
            case DELIVERED, POD_VERIFIED -> preferences.isNotifyDelivery();
            case DELAY_RISK, ETA_CHANGE -> preferences.isNotifyDelayRisk()
                    && meetsRiskFloor(preferences.getMinRiskLevel(), severity);
            case GENERAL -> true;
        };
    }

    /**
     * A CRITICAL alert always passes; a WARNING one is dropped for users who only
     * want to hear about critical delays.
     */
    private boolean meetsRiskFloor(DelayRiskLevel floor, NotificationSeverity severity) {
        if (floor == null || floor == DelayRiskLevel.LOW || floor == DelayRiskLevel.MEDIUM) {
            return true;
        }
        if (floor == DelayRiskLevel.HIGH) {
            return severity == NotificationSeverity.WARNING || severity == NotificationSeverity.CRITICAL;
        }
        return severity == NotificationSeverity.CRITICAL;
    }

    /* ===================== reading ===================== */

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> list(boolean unreadOnly, int limit) {
        User actor = currentUserService.getCurrentUser();
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(MAX_LIST_SIZE, limit)));

        Page<Notification> rows = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(actor.getId(), page)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(actor.getId(), page);

        return rows.getContent().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByRecipientIdAndReadFalse(
                currentUserService.getCurrentUser().getId());
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        User actor = currentUserService.getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        // someone else's alert is not theirs to read or even to discover
        if (!Objects.equals(notification.getRecipient().getId(), actor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllRead() {
        return notificationRepository.markAllRead(
                currentUserService.getCurrentUser().getId(), LocalDateTime.now());
    }

    /* ===================== settings ===================== */

    @Override
    @Transactional
    public NotificationPreferenceResponse getPreferences() {
        return toResponse(preferencesFor(currentUserService.getCurrentUser()));
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse updatePreferences(NotificationPreferenceRequest request) {
        User actor = currentUserService.getCurrentUser();
        NotificationPreference preferences = preferencesFor(actor);

        if (request.getInAppEnabled() != null) preferences.setInAppEnabled(request.getInAppEnabled());
        if (request.getEmailEnabled() != null) preferences.setEmailEnabled(request.getEmailEnabled());
        if (request.getNotifyStatusChange() != null) {
            preferences.setNotifyStatusChange(request.getNotifyStatusChange());
        }
        if (request.getNotifyDelayRisk() != null) preferences.setNotifyDelayRisk(request.getNotifyDelayRisk());
        if (request.getNotifyDelivery() != null) preferences.setNotifyDelivery(request.getNotifyDelivery());
        if (request.getMinRiskLevel() != null) {
            preferences.setMinRiskLevel(DelayRiskLevel.valueOf(request.getMinRiskLevel()));
        }

        if (Boolean.TRUE.equals(request.getSmsEnabled())) {
            // no phone number means no SMS, whatever the switch says
            if (actor.getPhone() == null || actor.getPhone().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Add a phone number to your profile before enabling SMS alerts");
            }
            preferences.setSmsEnabled(true);
        } else if (request.getSmsEnabled() != null) {
            preferences.setSmsEnabled(false);
        }

        return toResponse(preferenceRepository.save(preferences));
    }

    private NotificationPreference preferencesFor(User user) {
        return preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> preferenceRepository.save(
                        NotificationPreference.builder().user(user).build()));
    }

    /* ===================== mapping helpers ===================== */

    private NotificationResponse toResponse(Notification notification) {
        Shipment shipment = notification.getShipment();
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType() != null ? notification.getType().name() : null)
                .severity(notification.getSeverity() != null ? notification.getSeverity().name() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .shipmentId(shipment != null ? shipment.getId() : null)
                .trackingNumber(shipment != null ? shipment.getTrackingNumber() : null)
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .emailSent(notification.getEmailSentAt() != null)
                .smsSent(notification.getSmsSentAt() != null)
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference preferences) {
        User user = preferences.getUser();
        return NotificationPreferenceResponse.builder()
                .inAppEnabled(preferences.isInAppEnabled())
                .emailEnabled(preferences.isEmailEnabled())
                .smsEnabled(preferences.isSmsEnabled())
                .notifyStatusChange(preferences.isNotifyStatusChange())
                .notifyDelayRisk(preferences.isNotifyDelayRisk())
                .notifyDelivery(preferences.isNotifyDelivery())
                .minRiskLevel(preferences.getMinRiskLevel() != null
                        ? preferences.getMinRiskLevel().name() : DelayRiskLevel.HIGH.name())
                .emailChannelAvailable(emailSender.isConfigured())
                .smsChannelAvailable(smsSender.isConfigured())
                .phone(user != null ? user.getPhone() : null)
                .build();
    }

    private static String signature(Shipment shipment) {
        return "\n\nTrack this shipment with reference "
                + (shipment != null ? shipment.getTrackingNumber() : "")
                + ".\n— ShipTrack Pro";
    }

    private static String readable(ShipmentStatus status) {
        return status == null ? "updated" : status.name().toLowerCase().replace('_', ' ');
    }

    private static String humaniseDelay(int minutes) {
        int days = minutes / (24 * 60);
        int hours = (minutes % (24 * 60)) / 60;
        if (days > 0) {
            return days + (days == 1 ? " day" : " days") + (hours > 0 ? " " + hours + "h" : "");
        }
        if (hours > 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        }
        return minutes + " min";
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private static String nullSafe(String value) {
        return value == null ? "the receiver" : value;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }

    /** A destination for one alert; user is null for a contact without an account. */
    private record Recipient(User user, String email, String phone) {
    }
}
