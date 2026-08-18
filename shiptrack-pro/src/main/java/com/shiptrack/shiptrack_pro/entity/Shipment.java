package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "shipments",
        indexes = {
                @Index(name = "idx_shipments_tracking_number", columnList = "tracking_number", unique = true),
                @Index(name = "idx_shipments_status", columnList = "status"),
                @Index(name = "idx_shipments_created_by", columnList = "created_by")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public, human-quotable identifier, e.g. STP1A2B3C4D5E. Generated at creation. */
    @Column(name = "tracking_number", nullable = false, unique = true, updatable = false, length = 32)
    private String trackingNumber;

    /* ---------- ownership: every shipment is tied to real users ---------- */

    /** The authenticated user who created the shipment. Never taken from the request body. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    /**
     * Owning business account. Held as a plain id until the BUSINESS_ACCOUNTS
     * table exists, then promoted to a @ManyToOne relationship.
     */
    @Column(name = "business_id")
    private Long businessId;

    /** Logistics operator responsible for moving the shipment. Assigned later. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_operator_id")
    private User assignedOperator;

    /* ---------- sender ---------- */

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "sender_phone", nullable = false, length = 20)
    private String senderPhone;

    @Column(name = "sender_address", nullable = false, length = 500)
    private String senderAddress;

    /* ---------- receiver ---------- */

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @Column(name = "receiver_email")
    private String receiverEmail;

    @Column(name = "receiver_address", nullable = false, length = 500)
    private String receiverAddress;

    /* ---------- route endpoints ---------- */

    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    /* ---------- lifecycle ---------- */

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ShipmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private ShipmentPriority priority;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    /* ---------- packages ---------- */

    @Builder.Default
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShipmentPackage> packages = new ArrayList<>();

    /* ---------- cancellation ---------- */

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /* ---------- audit ---------- */

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* ---------- helpers ---------- */

    public void addPackage(ShipmentPackage shipmentPackage) {
        shipmentPackage.setShipment(this);
        this.packages.add(shipmentPackage);
    }

    public void clearPackages() {
        this.packages.clear();
    }
}