package com.shiptrack.shiptrack_pro.dto;

import lombok.*;
import java.time.LocalDateTime;

// Used for the queue list - deliberately excludes the base64 image payloads
// so the list endpoint stays fast/light. Fetch PodResponse for the full detail.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodSummaryResponse {
    private Long id;
    private Long shipmentId;
    private String trackingNumber;
    private String receiverName;
    private String submittedByName;
    private String status;
    private LocalDateTime submittedAt;
}
