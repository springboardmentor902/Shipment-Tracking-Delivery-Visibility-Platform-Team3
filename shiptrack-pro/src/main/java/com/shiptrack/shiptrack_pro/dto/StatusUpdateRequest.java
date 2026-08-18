package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    /**
     * Target status. Must be one of the closed set:
     * CREATED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED_DELIVERY, CANCELLED.
     */
    @NotBlank(message = "Target status is required")
    private String status;

    /** Optional free-text note recorded with the transition. */
    private String notes;
}