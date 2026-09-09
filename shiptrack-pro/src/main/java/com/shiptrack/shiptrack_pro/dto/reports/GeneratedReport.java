package com.shiptrack.shiptrack_pro.dto.reports;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedReport {
    private byte[] content;
    private String filename;
    private String contentType;
}
