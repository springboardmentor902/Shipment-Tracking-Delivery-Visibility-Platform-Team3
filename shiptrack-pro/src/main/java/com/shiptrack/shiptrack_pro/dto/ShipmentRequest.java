package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * Payload for creating a shipment.
 *
 * Deliberately does NOT accept trackingNumber, status, createdBy or businessId —
 * those are all derived by the backend from the authenticated user.
 */
@Data
public class ShipmentRequest {

    /* sender */

    @NotBlank(message = "Sender name is required")
    private String senderName;

    @NotBlank(message = "Sender phone is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Sender phone must be a valid phone number")
    private String senderPhone;

    @NotBlank(message = "Sender address is required")
    private String senderAddress;

    /* receiver */

    @NotBlank(message = "Receiver name is required")
    private String receiverName;

    @NotBlank(message = "Receiver phone is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Receiver phone must be a valid phone number")
    private String receiverPhone;

    @Email(message = "Receiver email must be valid")
    private String receiverEmail;

    @NotBlank(message = "Receiver address is required")
    private String receiverAddress;

    /* route */

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    /** STANDARD or EXPRESS. Defaults to STANDARD when omitted. */
    private String priority;

    /* packages */

    @NotEmpty(message = "A shipment must contain at least one package")
    @Valid
    private List<PackageRequest> packages;
}