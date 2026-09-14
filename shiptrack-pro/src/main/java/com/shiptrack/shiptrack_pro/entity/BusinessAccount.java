package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_accounts")
@Getter
@Setter
public class BusinessAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "contact_person", nullable = false)
    private String contactPerson;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "billing_address", nullable = false)
    private String billingAddress;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void beforeCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
    @PreUpdate
    public void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }
}