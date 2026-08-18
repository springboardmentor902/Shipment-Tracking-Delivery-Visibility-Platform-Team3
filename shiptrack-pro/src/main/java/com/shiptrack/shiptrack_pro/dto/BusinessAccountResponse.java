package com.shiptrack.shiptrack_pro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessAccountResponse {
    private Long id;
    private String companyName;
    private String gstNumber;
    private String contactPerson;
    private String contactPhone;
    private String billingAddress;
    private Long ownerId;
    private String ownerName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
