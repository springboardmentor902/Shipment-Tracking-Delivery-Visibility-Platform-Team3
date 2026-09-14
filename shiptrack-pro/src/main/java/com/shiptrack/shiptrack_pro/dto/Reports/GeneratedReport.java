package com.shiptrack.shiptrack_pro.dto.Reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedReport {

    private byte[] content;

    private String filename;

    private String contentType;
}