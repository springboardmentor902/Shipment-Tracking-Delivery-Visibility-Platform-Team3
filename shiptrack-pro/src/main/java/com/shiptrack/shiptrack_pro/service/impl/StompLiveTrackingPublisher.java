package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.LiveTrackingUpdate;
import com.shiptrack.shiptrack_pro.dto.TrackingEventResponse;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.service.LiveTrackingPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * STOMP implementation of the live feed.
 *
 * Per-shipment updates go to /topic/shipments/{id}, which only authorized
 * subscribers can join (see StompAuthChannelInterceptor). Operators, support
 * agents and admins additionally get a fleet feed on /topic/monitoring/active.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StompLiveTrackingPublisher implements LiveTrackingPublisher {

    public static final String SHIPMENT_TOPIC_PREFIX = "/topic/shipments/";
    public static final String FLEET_TOPIC = "/topic/monitoring/active";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishLocation(Shipment shipment, TrackingEventResponse event) {
        send(shipment, toUpdate(shipment, event, LiveTrackingUpdate.TYPE_LOCATION));
    }

    @Override
    public void publishCheckpoint(Shipment shipment, TrackingEventResponse event) {
        send(shipment, toUpdate(shipment, event, LiveTrackingUpdate.TYPE_CHECKPOINT));
    }

    @Override
    public void publishStatus(Shipment shipment, String notes, String actorName) {
        send(shipment, LiveTrackingUpdate.builder()
                .type(LiveTrackingUpdate.TYPE_STATUS)
                .shipmentId(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus() == null ? null : shipment.getStatus().name())
                .notes(notes)
                .recordedByName(actorName)
                .recordedAt(LocalDateTime.now())
                .build());
    }

    private LiveTrackingUpdate toUpdate(Shipment shipment, TrackingEventResponse event, String type) {
        return LiveTrackingUpdate.builder()
                .type(type)
                .shipmentId(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .status(event.getStatus())
                .location(event.getLocation())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .notes(event.getNotes())
                .recordedByName(event.getRecordedByName())
                .recordedAt(event.getRecordedAt() == null ? LocalDateTime.now() : event.getRecordedAt())
                .build();
    }

    /**
     * A failing broker is logged and swallowed: the tracking event is already
     * persisted, so losing a push must not roll the write back.
     */
    private void send(Shipment shipment, LiveTrackingUpdate update) {
        try {
            messagingTemplate.convertAndSend(SHIPMENT_TOPIC_PREFIX + shipment.getId(), update);
            messagingTemplate.convertAndSend(FLEET_TOPIC, update);
        } catch (RuntimeException exception) {
            log.warn("Could not broadcast live update for shipment {}: {}",
                    shipment.getId(), exception.getMessage());
        }
    }
}
