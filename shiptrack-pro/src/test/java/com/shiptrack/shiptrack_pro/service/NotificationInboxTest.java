package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.NotificationPreferenceRequest;
import com.shiptrack.shiptrack_pro.dto.NotificationPreferenceResponse;
import com.shiptrack.shiptrack_pro.dto.NotificationResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Covers the inbox side: a user reads and clears their own alerts, and nobody
 * else's.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Notification inbox and settings")
class NotificationInboxTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private AlertSender emailSender;
    @Mock private AlertSender smsSender;

    private NotificationServiceImpl service;

    private User actor;
    private User otherUser;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository, preferenceRepository,
                userRepository, currentUserService, emailSender, smsSender, 30);

        actor = user(1L, "Anita Business", "anita@acme.test", "+919000000001");
        otherUser = user(2L, "Someone Else", "else@acme.test", "+919000000002");

        when(currentUserService.getCurrentUser()).thenReturn(actor);
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("the listing is scoped to the signed-in user")
    void listReturnsOwnNotifications() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page(List.of(notification(10L, false))));

        List<NotificationResponse> result = service.list(false, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getTrackingNumber()).isEqualTo("STP-99");
        verify(notificationRepository).findByRecipientIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
        verify(notificationRepository, never())
                .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("unreadOnly uses the unread query")
    void listCanFilterUnread() {
        when(notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page(List.of(notification(11L, false))));

        service.list(true, 20);

        verify(notificationRepository)
                .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("the page size is clamped so a huge limit cannot be requested")
    void listSizeIsClamped() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page(List.of()));

        service.list(false, 100_000);

        verify(notificationRepository).findByRecipientIdOrderByCreatedAtDesc(eq(1L),
                argThat(pageable -> pageable.getPageSize() <= 100));
    }

    @Test
    @DisplayName("marking read stamps the time once")
    void markReadStampsTime() {
        Notification notification = notification(12L, false);
        when(notificationRepository.findById(12L)).thenReturn(Optional.of(notification));

        NotificationResponse response = service.markRead(12L);

        assertThat(response.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("marking an already read alert does not rewrite it")
    void markReadIsIdempotent() {
        Notification notification = notification(13L, true);
        LocalDateTime originalReadAt = LocalDateTime.now().minusHours(3);
        notification.setReadAt(originalReadAt);
        when(notificationRepository.findById(13L)).thenReturn(Optional.of(notification));

        service.markRead(13L);

        assertThat(notification.getReadAt()).isEqualTo(originalReadAt);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("someone else's alert is reported as not found, not as forbidden")
    void cannotReadAnotherUsersNotification() {
        Notification foreign = notification(14L, false);
        foreign.setRecipient(otherUser);
        when(notificationRepository.findById(14L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.markRead(14L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        assertThat(foreign.isRead()).isFalse();
    }

    @Test
    @DisplayName("a missing alert is a 404")
    void missingNotificationIsNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(99L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("mark all read only touches the caller's rows")
    void markAllReadIsScoped() {
        when(notificationRepository.markAllRead(eq(1L), any(LocalDateTime.class))).thenReturn(4);

        assertThat(service.markAllRead()).isEqualTo(4);
        verify(notificationRepository).markAllRead(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("the unread count is scoped to the caller")
    void unreadCountIsScoped() {
        when(notificationRepository.countByRecipientIdAndReadFalse(1L)).thenReturn(7L);

        assertThat(service.unreadCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("first read of preferences creates conservative defaults")
    void defaultsAreCreatedOnFirstRead() {
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        NotificationPreferenceResponse response = service.getPreferences();

        assertThat(response.isInAppEnabled()).isTrue();
        assertThat(response.isEmailEnabled()).isTrue();
        // SMS costs money, so it stays off until asked for
        assertThat(response.isSmsEnabled()).isFalse();
        assertThat(response.getMinRiskLevel()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("an update changes only the fields sent")
    void updateIsPartial() {
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(
                NotificationPreference.builder().user(actor)
                        .notifyStatusChange(true).notifyDelayRisk(true).build()));

        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setNotifyStatusChange(false);

        NotificationPreferenceResponse response = service.updatePreferences(request);

        assertThat(response.isNotifyStatusChange()).isFalse();
        assertThat(response.isNotifyDelayRisk()).isTrue();
    }

    @Test
    @DisplayName("the response reports whether each channel is actually configured")
    void channelAvailabilityIsReported() {
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(emailSender.isConfigured()).thenReturn(true);
        when(smsSender.isConfigured()).thenReturn(false);

        NotificationPreferenceResponse response = service.getPreferences();

        assertThat(response.isEmailChannelAvailable()).isTrue();
        assertThat(response.isSmsChannelAvailable()).isFalse();
        assertThat(response.getPhone()).isEqualTo("+919000000001");
    }

    @Test
    @DisplayName("SMS can be enabled when the profile has a phone number")
    void smsCanBeEnabledWithPhone() {
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(
                NotificationPreference.builder().user(actor).build()));

        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setSmsEnabled(true);

        assertThat(service.updatePreferences(request).isSmsEnabled()).isTrue();
    }

    /* ---- helpers ---- */

    private Page<Notification> page(List<Notification> content) {
        return new PageImpl<>(content, PageRequest.of(0, 20), content.size());
    }

    private Notification notification(Long id, boolean read) {
        Shipment shipment = Shipment.builder().id(99L).trackingNumber("STP-99").build();
        return Notification.builder()
                .id(id)
                .recipient(actor)
                .shipment(shipment)
                .type(NotificationType.STATUS_CHANGE)
                .severity(NotificationSeverity.INFO)
                .title("STP-99 is now in transit")
                .message("Shipment STP-99 is now in transit.")
                .read(read)
                .createdAt(LocalDateTime.now())
                .build();
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
