package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {

    private Long id;
    private String trackingNumber;

    /* ownership, flattened so the frontend never needs a second call */
    private Long createdById;
    private String createdByName;
    private String createdByRole;
    private Long businessId;
    private Long assignedOperatorId;
    private String assignedOperatorName;

    /* sender */
    private String senderName;
    private String senderPhone;
    private String senderAddress;

    /* receiver */
    private String receiverName;
    private String receiverPhone;
    private String receiverEmail;
    private String receiverAddress;

    /* route */
    private String pickupAddress;
    private String deliveryAddress;

    /* lifecycle */
    private String status;
    private String priority;

    /** Statuses this shipment may legally move to next. Empty when terminal. */
    private Set<String> allowedNextStatuses;

    private LocalDate estimatedDeliveryDate;
    private LocalDate actualDeliveryDate;

    /* packages */
    private List<PackageResponse> packages;
    private Integer totalPackages;

    /* cancellation */
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    /* audit */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}