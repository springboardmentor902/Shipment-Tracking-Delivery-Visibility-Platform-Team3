package com.shiptrack.shiptrack_pro.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BusinessAccountResponse {

    private Long id;

    private String companyName;

    private String gstNumber;

    private String contactPerson;

    private String contactPhone;

    private String billingAddress;

    private String createdBy;

    private String ownerName;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}