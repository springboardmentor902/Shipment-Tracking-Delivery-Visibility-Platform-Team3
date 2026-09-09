package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "proof_of_deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One proof of delivery per shipment
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    // Driver who submitted the proof
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    // Recipient signature, stored as a base64 data URL (no external storage needed)
    @Lob
    @Column(name = "signature_image", columnDefinition = "TEXT", nullable = false)
    private String signatureImage;

    // Delivery photo, stored as a base64 data URL
    @Lob
    @Column(name = "photo_image", columnDefinition = "TEXT", nullable = false)
    private String photoImage;

    private String receiverName;

    private String notes;

    // PENDING | APPROVED | REJECTED
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    // Admin/support agent who verified it
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    private LocalDateTime verifiedAt;

    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
}
