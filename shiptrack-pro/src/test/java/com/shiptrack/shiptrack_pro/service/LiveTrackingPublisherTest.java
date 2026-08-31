package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.LiveTrackingUpdate;
import com.shiptrack.shiptrack_pro.dto.TrackingEventResponse;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.service.impl.StompLiveTrackingPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LiveTrackingPublisherTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    private Shipment shipment() {
        return Shipment.builder()
                .id(42L)
                .trackingNumber("STP9876543210")
                .status(ShipmentStatus.IN_TRANSIT)
                .build();
    }

    private TrackingEventResponse event() {
        return TrackingEventResponse.builder()
                .id(7L)
                .status("IN_TRANSIT")
                .location("Uppal, Hyderabad")
                .latitude(new BigDecimal("17.412300"))
                .longitude(new BigDecimal("78.501200"))
                .recordedByName("Ravi Operator")
                .recordedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("a location ping reaches the shipment topic and the fleet feed")
    void locationGoesToBothTopics() {
        new StompLiveTrackingPublisher(messagingTemplate).publishLocation(shipment(), event());

        ArgumentCaptor<String> destinations = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloads = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, org.mockito.Mockito.times(2))
                .convertAndSend(destinations.capture(), payloads.capture());

        assertEquals("/topic/shipments/42", destinations.getAllValues().get(0));
        assertEquals("/topic/monitoring/active", destinations.getAllValues().get(1));

        LiveTrackingUpdate update = (LiveTrackingUpdate) payloads.getAllValues().get(0);
        assertEquals(LiveTrackingUpdate.TYPE_LOCATION, update.getType());
        assertEquals(42L, update.getShipmentId());
        assertEquals("Uppal, Hyderabad", update.getLocation());
        assertEquals(new BigDecimal("17.412300"), update.getLatitude());
    }

    @Test
    @DisplayName("a status change is published with the actor's name")
    void statusChangeIsPublished() {
        new StompLiveTrackingPublisher(messagingTemplate)
                .publishStatus(shipment(), "left the Hyderabad hub", "Ravi Operator");

        ArgumentCaptor<Object> payloads = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, org.mockito.Mockito.times(2)).convertAndSend(anyString(), payloads.capture());

        LiveTrackingUpdate update = (LiveTrackingUpdate) payloads.getAllValues().get(0);
        assertEquals(LiveTrackingUpdate.TYPE_STATUS, update.getType());
        assertEquals("IN_TRANSIT", update.getStatus());
        assertEquals("left the Hyderabad hub", update.getNotes());
        assertEquals("Ravi Operator", update.getRecordedByName());
    }

    @Test
    @DisplayName("a broken broker never fails the write that produced the update")
    void brokerFailureIsSwallowed() {
        doThrow(new MessagingException("broker down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        StompLiveTrackingPublisher publisher = new StompLiveTrackingPublisher(messagingTemplate);

        assertDoesNotThrow(() -> publisher.publishCheckpoint(shipment(), event()));
    }
}
