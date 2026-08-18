package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @Size(min = 1, max = 255, message = "Full name must be between 1 and 255 characters")
    private String fullName;

    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Phone must be a valid phone number")
    private String phone;

    @Pattern(regexp = "^https?://.+$", message = "Profile image URL must start with http:// or https://")
    @Size(max = 500, message = "Profile image URL cannot exceed 500 characters")
    private String profileImageUrl;
}
