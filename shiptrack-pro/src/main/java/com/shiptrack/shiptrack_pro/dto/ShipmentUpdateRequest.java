package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * Payload for editing an existing shipment. Every field is optional —
 * only non-null fields are applied. Status is NOT editable here; use the
 * dedicated status transition endpoint instead.
 */
@Data
public class ShipmentUpdateRequest {

    private String senderName;

    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Sender phone must be a valid phone number")
    private String senderPhone;

    private String senderAddress;

    private String receiverName;

    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Receiver phone must be a valid phone number")
    private String receiverPhone;

    @Email(message = "Receiver email must be valid")
    private String receiverEmail;

    private String receiverAddress;

    private String pickupAddress;

    private String deliveryAddress;

    /** STANDARD or EXPRESS. */
    private String priority;

    /** Id of the logistics operator to assign. Administrators and operators only. */
    private Long assignedOperatorId;

    /** When provided, replaces the whole package list. Omit to leave packages untouched. */
    @Valid
    private List<PackageRequest> packages;
}