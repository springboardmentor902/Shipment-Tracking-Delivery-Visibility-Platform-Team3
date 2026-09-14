package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.BusinessAccountRequest;
import com.shiptrack.shiptrack_pro.entity.BusinessAccount;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.BusinessAccountRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.BusinessAccountService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessAccountServiceImpl
        implements BusinessAccountService {

    private final BusinessAccountRepository businessAccountRepository;
    private final UserRepository userRepository;

    @Override
    public List<BusinessAccount> findAll() {

        List<BusinessAccount> accounts =
                businessAccountRepository.findAll();

        for (BusinessAccount account : accounts) {

            if (account.getCreatedBy() == null) {
                continue;
            }

            userRepository.findByEmail(account.getCreatedBy())
                    .ifPresent(user -> {

                        if (account.getContactPerson() == null ||
                                account.getContactPerson().isBlank()) {

                            account.setContactPerson(
                                    user.getFullName()
                            );
                        }

                        if (account.getContactPhone() == null ||
                                account.getContactPhone().isBlank()) {

                            account.setContactPhone(
                                    user.getPhone()
                            );
                        }

                        businessAccountRepository.save(account);
                    });
        }

        return businessAccountRepository.findAll();
    }
    @Override
    public BusinessAccount getMyAccount() {

        String loggedInUsername = getLoggedInUsername();

        return businessAccountRepository
                .findByCreatedBy(loggedInUsername)
                .orElse(null);
    }

    @Override
    public BusinessAccount create(
            BusinessAccountRequest request) {

        String loggedInUsername = getLoggedInUsername();

        User user = userRepository
                .findByEmail(loggedInUsername)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Logged-in user not found"));

        BusinessAccount account = new BusinessAccount();

        account.setCompanyName(
                request.getCompanyName());

        account.setGstNumber(
                request.getGstNumber());

        // Automatically from users table
        account.setContactPerson(
                user.getFullName());

        account.setContactPhone(
                user.getPhone());

        // Billing address is still from request
        account.setBillingAddress(
                request.getBillingAddress());

        // Automatically from logged-in user
        account.setCreatedBy(
                user.getEmail());
        account.setBillingAddress(
                user.getAddress()
        );
        return businessAccountRepository.save(account);
    }

    @Override
    public BusinessAccount updateMyAccount(
            BusinessAccountRequest request) {

        String loggedInUsername = getLoggedInUsername();

        BusinessAccount account =
                businessAccountRepository
                        .findByCreatedBy(loggedInUsername)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Business account not found"));

        User user = userRepository
                .findByEmail(loggedInUsername)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Logged-in user not found"));

        account.setCompanyName(
                request.getCompanyName());

        account.setGstNumber(
                request.getGstNumber());

        // Automatically from users table
        account.setContactPerson(
                user.getFullName());

        account.setContactPhone(
                user.getPhone());

        account.setBillingAddress(
                request.getBillingAddress());

        // Do NOT change createdBy here

        return businessAccountRepository.save(account);
    }

    private String getLoggedInUsername() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated");
        }

        return authentication.getName();
    }
}