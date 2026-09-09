package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodVerifyRequest {

    // APPROVED or REJECTED
    @NotNull
    @Pattern(regexp = "APPROVED|REJECTED", message = "decision must be APPROVED or REJECTED")
    private String decision;

    // Required when rejecting; optional otherwise
    private String rejectionReason;
}
