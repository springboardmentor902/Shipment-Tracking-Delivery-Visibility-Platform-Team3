package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    List<TrackingEvent> findByShipmentOrderByRecordedAtAsc(Shipment shipment);

    Optional<TrackingEvent> findFirstByShipmentOrderByRecordedAtDesc(Shipment shipment);
}
