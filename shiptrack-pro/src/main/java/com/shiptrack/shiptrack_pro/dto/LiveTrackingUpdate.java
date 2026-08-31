package com.shiptrack.shiptrack_pro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload pushed over STOMP to everyone watching a shipment.
 *
 * It carries enough data for the browser to move the driver marker and prepend a
 * timeline row without calling the REST API again.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveTrackingUpdate {

    /** LOCATION, CHECKPOINT or STATUS. */
    private String type;

    private Long shipmentId;
    private String trackingNumber;
    private String status;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
    private String recordedByName;
    private LocalDateTime recordedAt;

    public static final String TYPE_LOCATION = "LOCATION";
    public static final String TYPE_CHECKPOINT = "CHECKPOINT";
    public static final String TYPE_STATUS = "STATUS";
}
