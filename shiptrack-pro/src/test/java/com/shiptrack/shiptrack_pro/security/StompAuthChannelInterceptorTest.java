package com.shiptrack.shiptrack_pro.security;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The live tracking socket must be no more open than the REST API: a valid login
 * is required to connect, and a subscription only succeeds for a shipment the
 * user is already allowed to read.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StompAuthChannelInterceptorTest {

    private static final String TOKEN = "a.valid.token";

    @Mock private JwtUtil jwtUtil;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private UserRepository userRepository;
    @Mock private ShipmentRepository shipmentRepository;

    // the real policy: this test is about the socket honouring the actual rules
    private final ShipmentAccessPolicy accessPolicy = new ShipmentAccessPolicy();

    @InjectMocks private StompAuthChannelInterceptor interceptor;

    private User operator;
    private User outsider;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthChannelInterceptor(
                jwtUtil, userDetailsService, userRepository, shipmentRepository, accessPolicy);

        operator = user(5L, "ravi@shiptrack.test", Role.LOGISTICS_OPERATOR);
        outsider = user(9L, "nosy@shiptrack.test", Role.CUSTOMER);

        shipment = Shipment.builder()
                .id(42L)
                .trackingNumber("STP9876543210")
                .createdBy(operator)
                .assignedOperator(operator)
                .receiverEmail("receiver@shiptrack.test")
                .build();

        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipment));
        when(userRepository.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator));
        when(userRepository.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider));

        when(jwtUtil.extractEmail(TOKEN)).thenReturn(operator.getEmail());
        when(jwtUtil.isTokenValid(anyString(), anyString())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(operator.getEmail()))
                .thenReturn(userDetails(operator));
    }

    private User user(Long id, String email, Role role) {
        User user = User.builder().id(id).email(email).fullName(email).build();
        user.setRole(role.name());
        return user;
    }

    private UserDetails userDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), "hashed",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }

    private Message<byte[]> connectFrame(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeFrame(String destination, User as) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (as != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(as.getEmail(), null, List.of()));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("a CONNECT without a token is rejected")
    void connectWithoutTokenIsRejected() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(connectFrame(null), null));
    }

    @Test
    @DisplayName("a CONNECT with an unknown token is rejected")
    void connectWithBadTokenIsRejected() {
        when(jwtUtil.extractEmail("broken")).thenThrow(new IllegalArgumentException("bad signature"));

        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(connectFrame("Bearer broken"), null));
    }

    @Test
    @DisplayName("a CONNECT for a deleted account is rejected")
    void connectForMissingUserIsRejected() {
        when(userDetailsService.loadUserByUsername(operator.getEmail()))
                .thenThrow(new UsernameNotFoundException("gone"));

        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(connectFrame("Bearer " + TOKEN), null));
    }

    @Test
    @DisplayName("a CONNECT with a valid token pins the user to the session")
    void connectWithValidTokenSucceeds() {
        Message<byte[]> frame = connectFrame("Bearer " + TOKEN);

        Message<?> result = interceptor.preSend(frame, null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNotNull(accessor.getUser());
        assertEquals(operator.getEmail(), accessor.getUser().getName());
    }

    @Test
    @DisplayName("the assigned operator may subscribe to their shipment")
    void assignedOperatorMaySubscribe() {
        Message<?> result = interceptor.preSend(
                subscribeFrame("/topic/shipments/42", operator), null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("an unrelated customer cannot subscribe to someone else's shipment")
    void outsiderCannotSubscribe() {
        MessageDeliveryException error = assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(subscribeFrame("/topic/shipments/42", outsider), null));

        assertEquals("You do not have access to this shipment", error.getMessage());
    }

    @Test
    @DisplayName("an anonymous session cannot subscribe at all")
    void anonymousCannotSubscribe() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(subscribeFrame("/topic/shipments/42", null), null));
    }

    @Test
    @DisplayName("a customer cannot watch the fleet feed")
    void customerCannotWatchFleetFeed() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(subscribeFrame("/topic/monitoring/active", outsider), null));
    }

    @Test
    @DisplayName("an operator can watch the fleet feed")
    void operatorCanWatchFleetFeed() {
        assertNotNull(interceptor.preSend(subscribeFrame("/topic/monitoring/active", operator), null));
    }

    @Test
    @DisplayName("destinations outside the tracking topics are refused")
    void unknownDestinationIsRefused() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(subscribeFrame("/topic/secrets", operator), null));
    }

    @Test
    @DisplayName("a malformed shipment topic is refused")
    void malformedTopicIsRefused() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(subscribeFrame("/topic/shipments/abc", operator), null));
    }
}
