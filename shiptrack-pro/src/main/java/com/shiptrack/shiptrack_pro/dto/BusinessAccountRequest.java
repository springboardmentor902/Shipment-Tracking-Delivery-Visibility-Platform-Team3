package com.shiptrack.shiptrack_pro.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessAccountRequest {

    private String companyName;

    private String gstNumber;

    private String contactPerson;

    private String contactPhone;

    private String billingAddress;
}