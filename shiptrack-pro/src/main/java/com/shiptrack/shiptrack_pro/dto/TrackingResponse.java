package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingResponse {

    private ShipmentResponse shipment;
    private List<TrackingEventResponse> events;
}
