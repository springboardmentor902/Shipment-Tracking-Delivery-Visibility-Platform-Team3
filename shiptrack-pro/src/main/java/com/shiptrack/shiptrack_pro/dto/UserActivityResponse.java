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
public class UserActivityResponse {
    private Long id;
    private Long userId;
    private String action;
    private String detail;
    private String ipAddress;
    private LocalDateTime createdAt;
}
