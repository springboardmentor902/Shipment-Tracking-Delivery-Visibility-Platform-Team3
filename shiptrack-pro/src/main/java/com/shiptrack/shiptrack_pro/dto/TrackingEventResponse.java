package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEventResponse {

    private Long id;
    private String status;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
    private String recordedByName;
    private LocalDateTime recordedAt;
}
