package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.BusinessAccountRequest;
import com.shiptrack.shiptrack_pro.entity.BusinessAccount;

import java.util.List;

public interface BusinessAccountService {

    List<BusinessAccount> findAll();

    BusinessAccount getMyAccount();

    BusinessAccount create(
            BusinessAccountRequest request
    );

    BusinessAccount updateMyAccount(
            BusinessAccountRequest request
    );
}