package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodSubmitRequest {

    @NotBlank
    private String signatureImage; // base64 data URL

    @NotBlank
    private String photoImage; // base64 data URL

    private String receiverName;

    private String notes;
}
