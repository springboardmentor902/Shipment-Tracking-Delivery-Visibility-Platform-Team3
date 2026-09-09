package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ETAPredictionRequest {

    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;
}