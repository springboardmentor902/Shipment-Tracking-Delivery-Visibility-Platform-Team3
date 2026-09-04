package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "proof_of_delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProofOfDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "signature_url")
    private String signatureUrl;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "delivered_to_name")
    private String deliveredToName;

    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    @Column(name = "verification_status")
    private String verificationStatus;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}