package com.shiptrack.shiptrack_pro.security;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

/**
 * Guards the live tracking WebSocket.
 *
 * CONNECT frames must carry the same JWT the REST API uses, and every SUBSCRIBE
 * is checked against the shipment access rules — otherwise anyone with a valid
 * login could listen to a stranger's delivery by guessing a shipment id.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String SHIPMENT_TOPIC_PREFIX = "/topic/shipments/";
    private static final String FLEET_TOPIC = "/topic/monitoring/active";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentAccessPolicy accessPolicy;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticate(accessor);
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    /** Reads the JWT from the CONNECT frame and pins the user to the session. */
    private void authenticate(StompHeaderAccessor accessor) {
        String token = readToken(accessor);
        if (token == null) {
            throw new MessageDeliveryException("A JWT is required to open the tracking socket");
        }

        try {
            String email = jwtUtil.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                throw new MessageDeliveryException("The tracking socket token has expired");
            }
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);
        } catch (MessageDeliveryException exception) {
            throw exception;
        } catch (Exception exception) {
            log.debug("Rejected a tracking socket connection: {}", exception.getMessage());
            throw new MessageDeliveryException("The tracking socket token is not valid");
        }
    }

    /** Only lets a session listen to shipments it is already allowed to read. */
    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new MessageDeliveryException("A subscription destination is required");
        }

        User user = resolveUser(accessor.getUser());

        if (destination.equals(FLEET_TOPIC)) {
            if (!accessPolicy.canMonitorFleet(user)) {
                throw new MessageDeliveryException("Your role cannot watch the fleet feed");
            }
            return;
        }

        if (destination.startsWith(SHIPMENT_TOPIC_PREFIX)) {
            Shipment shipment = shipmentRepository.findById(parseShipmentId(destination))
                    .orElseThrow(() -> new MessageDeliveryException("That shipment does not exist"));
            if (!accessPolicy.canView(shipment, user)) {
                throw new MessageDeliveryException("You do not have access to this shipment");
            }
            return;
        }

        // Anything not explicitly allowed stays closed.
        throw new MessageDeliveryException("Unknown subscription destination: " + destination);
    }

    private Long parseShipmentId(String destination) {
        String raw = destination.substring(SHIPMENT_TOPIC_PREFIX.length());
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException exception) {
            throw new MessageDeliveryException("Malformed shipment topic: " + destination);
        }
    }

    private User resolveUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new MessageDeliveryException("The tracking socket session is not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new MessageDeliveryException("Authenticated user no longer exists"));
    }

    /**
     * Browsers cannot set headers on the WebSocket handshake, so the token comes
     * in on the STOMP CONNECT frame instead — as "Authorization: Bearer x" or a
     * plain "token" header.
     */
    private String readToken(StompHeaderAccessor accessor) {
        String authorization = Optional.ofNullable(accessor.getFirstNativeHeader("Authorization"))
                .orElse(accessor.getFirstNativeHeader("authorization"));
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }

        String token = Optional.ofNullable(accessor.getFirstNativeHeader("token"))
                .orElse(authorization);
        return token == null || token.isBlank() ? null : token.trim();
    }
}
