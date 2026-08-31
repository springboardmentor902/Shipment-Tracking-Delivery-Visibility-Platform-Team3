package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.NotificationPreferenceRequest;
import com.shiptrack.shiptrack_pro.entity.*;
import com.shiptrack.shiptrack_pro.repository.NotificationPreferenceRepository;
import com.shiptrack.shiptrack_pro.repository.NotificationRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers the dispatch rules: who gets told, which channels are used, and when an
 * alert is deliberately dropped.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Notification dispatch")
class NotificationDispatchTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private AlertSender emailSender;
    @Mock private AlertSender smsSender;

    private NotificationServiceImpl service;

    private User sender;
    private User operator;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository, preferenceRepository,
                userRepository, currentUserService, emailSender, smsSender, 30);

        sender = user(1L, "Anita Business", "anita@acme.test", "+919000000001");
        operator = user(2L, "Ravi Operator", "ravi@shiptrack.test", "+919000000002");

        shipment = Shipment.builder()
                .id(50L)
                .trackingNumber("STP-50")
                .status(ShipmentStatus.IN_TRANSIT)
                .receiverName("Meera Receiver")
                .receiverEmail("meera@customer.test")
                .receiverPhone("+919000000003")
                .createdBy(sender)
                .assignedOperator(operator)
                .build();

        // by default nobody has saved preferences yet, so defaults are created
        when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(emailSender.send(anyString(), anyString(), anyString())).thenReturn(true);
        when(smsSender.send(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("a status change reaches the booker in-app and by email")
    void statusChangeNotifiesBooker() {
        service.notifyStatusChange(shipment, "Left the Hyderabad hub", "Ravi Operator");

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(saved.capture());

        Notification forSender = saved.getAllValues().stream()
                .filter(n -> n.getRecipient().getId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(forSender.getType()).isEqualTo(NotificationType.STATUS_CHANGE);
        assertThat(forSender.getSeverity()).isEqualTo(NotificationSeverity.INFO);
        assertThat(forSender.getTitle()).contains("STP-50", "in transit");
        assertThat(forSender.getMessage()).contains("Left the Hyderabad hub");
        assertThat(forSender.isRead()).isFalse();
        assertThat(forSender.getEmailSentAt()).isNotNull();
        // an ordinary status update is not worth an SMS
        assertThat(forSender.getSmsSentAt()).isNull();
        verify(emailSender).send(eq("anita@acme.test"), anyString(), anyString());
    }

    @Test
    @DisplayName("a receiver without an account is still emailed, with no stored row")
    void receiverWithoutAccountIsContactedDirectly() {
        service.notifyStatusChange(shipment, null, null);

        verify(emailSender).send(eq("meera@customer.test"), anyString(), anyString());
        verify(notificationRepository, never()).save(argThat(n ->
                n != null && n.getRecipient() != null && "meera@customer.test".equals(n.getRecipient().getEmail())));
    }

    @Test
    @DisplayName("a status update does not go to the operator, a delay alert does")
    void operatorOnlyHearsAboutDelays() {
        service.notifyStatusChange(shipment, null, null);
        assertThat(savedRecipientIds()).doesNotContain(2L);

        reset(notificationRepository);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.notifyDelayRisk(shipment, DelayRiskLevel.HIGH, 62,
                LocalDateTime.now().plusDays(1), 300);

        assertThat(savedRecipientIds()).contains(2L);
    }

    @Test
    @DisplayName("the same alert inside the cooldown window is dropped")
    void repeatedAlertIsSuppressed() {
        when(notificationRepository.existsByRecipientIdAndDedupeKeyAndCreatedAtAfter(
                eq(1L), eq("DELAY:50:HIGH"), any(LocalDateTime.class))).thenReturn(true);

        service.notifyDelayRisk(shipment, DelayRiskLevel.HIGH, 62, null, null);

        assertThat(savedRecipientIds()).doesNotContain(1L);
    }

    @Test
    @DisplayName("switching off delay alerts stops them for that user only")
    void preferencesAreRespected() {
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(
                NotificationPreference.builder().user(sender).notifyDelayRisk(false).build()));

        service.notifyDelayRisk(shipment, DelayRiskLevel.CRITICAL, 90, null, null);

        assertThat(savedRecipientIds()).doesNotContain(1L).contains(2L);
    }

    @Test
    @DisplayName("a HIGH floor drops warnings but never critical alerts")
    void riskFloorFiltersWarnings() {
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(
                NotificationPreference.builder().user(sender)
                        .minRiskLevel(DelayRiskLevel.CRITICAL).build()));

        service.notifyDelayRisk(shipment, DelayRiskLevel.HIGH, 60, null, null);
        assertThat(savedRecipientIds()).doesNotContain(1L);

        service.notifyDelayRisk(shipment, DelayRiskLevel.CRITICAL, 88, null, null);
        assertThat(savedRecipientIds()).contains(1L);
    }

    @Test
    @DisplayName("SMS goes out for a delay only when the user has opted in")
    void smsRequiresOptIn() {
        service.notifyDelayRisk(shipment, DelayRiskLevel.CRITICAL, 91, null, null);
        verify(smsSender, never()).send(eq("anita@acme.test"), anyString(), anyString());
        verify(smsSender, never()).send(eq("+919000000001"), anyString(), anyString());

        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(
                NotificationPreference.builder().user(sender).smsEnabled(true)
                        .minRiskLevel(DelayRiskLevel.LOW).build()));

        service.notifyDelayRisk(shipment, DelayRiskLevel.CRITICAL, 91, null, null);
        verify(smsSender).send(eq("+919000000001"), anyString(), anyString());
    }

    @Test
    @DisplayName("in-app off still allows email, but stores nothing")
    void inAppCanBeDisabledIndependently() {
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(
                NotificationPreference.builder().user(sender).inAppEnabled(false).build()));

        service.notifyStatusChange(shipment, null, null);

        assertThat(savedRecipientIds()).doesNotContain(1L);
        verify(emailSender).send(eq("anita@acme.test"), anyString(), anyString());
    }

    @Test
    @DisplayName("a failing email channel does not break the alert")
    void senderFailureIsContained() {
        when(emailSender.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("smtp down"));

        service.notifyStatusChange(shipment, null, null);

        // the raise path swallows the failure; nothing propagates to the caller
        assertThat(savedRecipientIds()).isNotNull();
    }

    @Test
    @DisplayName("delivery alerts follow the delivery preference, not the status one")
    void deliveryUsesItsOwnSwitch() {
        shipment.setStatus(ShipmentStatus.DELIVERED);
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(
                NotificationPreference.builder().user(sender)
                        .notifyStatusChange(false).notifyDelivery(true).build()));

        service.notifyStatusChange(shipment, null, null);

        Notification forSender = savedNotifications().stream()
                .filter(n -> n.getRecipient().getId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertThat(forSender.getType()).isEqualTo(NotificationType.DELIVERED);
        assertThat(forSender.getTitle()).startsWith("Delivered:");
    }

    @Test
    @DisplayName("enabling SMS without a phone number is rejected")
    void smsNeedsPhoneNumber() {
        User noPhone = user(3L, "No Phone", "nophone@acme.test", null);
        when(currentUserService.getCurrentUser()).thenReturn(noPhone);

        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setSmsEnabled(true);

        assertThatThrownBy(() -> service.updatePreferences(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("phone number");
    }

    /* ---- helpers ---- */

    private List<Notification> savedNotifications() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    private List<Long> savedRecipientIds() {
        return savedNotifications().stream().map(n -> n.getRecipient().getId()).toList();
    }

    private static User user(Long id, String name, String email, String phone) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        return user;
    }
}
