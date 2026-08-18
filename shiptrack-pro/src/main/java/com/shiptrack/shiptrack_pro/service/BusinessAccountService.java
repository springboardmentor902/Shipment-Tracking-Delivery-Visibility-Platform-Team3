package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.BusinessAccountRequest;
import com.shiptrack.shiptrack_pro.dto.BusinessAccountResponse;

import java.util.List;

public interface BusinessAccountService {
    BusinessAccountResponse createMyAccount(BusinessAccountRequest request);
    BusinessAccountResponse getMyAccount();
    List<BusinessAccountResponse> getAllAccounts();
    BusinessAccountResponse updateMyAccount(BusinessAccountRequest request);
}
