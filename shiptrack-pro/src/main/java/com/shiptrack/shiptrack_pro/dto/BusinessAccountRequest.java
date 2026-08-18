package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BusinessAccountRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name cannot exceed 255 characters")
    private String companyName;

    @Size(max = 20, message = "GST number cannot exceed 20 characters")
    private String gstNumber;

    @Size(max = 255, message = "Contact person cannot exceed 255 characters")
    private String contactPerson;

    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Contact phone must be a valid phone number")
    private String contactPhone;

    @Size(max = 500, message = "Billing address cannot exceed 500 characters")
    private String billingAddress;
}
