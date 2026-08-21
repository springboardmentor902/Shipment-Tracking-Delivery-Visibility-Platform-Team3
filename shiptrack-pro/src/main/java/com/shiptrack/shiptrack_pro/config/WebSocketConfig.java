package com.shiptrack.shiptrack_pro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Clients connect here (SockJS fallback for browsers/proxies that
        // block raw WebSocket). Frontend origin is the Next.js dev server.
        registry.addEndpoint("/api/ws/tracking")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages the server pushes to clients go out under /topic/**
        // e.g. /topic/shipment/42/location
        registry.enableSimpleBroker("/topic");

        // Messages clients send to the server (not used yet, since the
        // driver posts location via REST, but kept for future STOMP-based
        // client -> server messages) would be prefixed with /app
        registry.setApplicationDestinationPrefixes("/app");
    }
}
