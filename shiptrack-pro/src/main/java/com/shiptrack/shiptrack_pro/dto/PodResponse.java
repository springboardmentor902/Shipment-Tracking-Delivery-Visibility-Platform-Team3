package com.shiptrack.shiptrack_pro.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodResponse {
    private Long id;
    private Long shipmentId;
    private String trackingNumber;
    private String receiverName;
    private String notes;
    private String signatureImage;
    private String photoImage;
    private String submittedByName;
    private String status;
    private LocalDateTime submittedAt;
    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
}
