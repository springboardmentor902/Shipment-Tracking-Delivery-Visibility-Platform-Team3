package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.BusinessAccountRequest;
import com.shiptrack.shiptrack_pro.dto.BusinessAccountResponse;
import com.shiptrack.shiptrack_pro.entity.BusinessAccount;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.BusinessAccountRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.BusinessAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessAccountServiceImpl implements BusinessAccountService {

    private final BusinessAccountRepository businessAccountRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public BusinessAccountResponse createMyAccount(BusinessAccountRequest request) {
        User owner = getBusinessOwner();

        if (businessAccountRepository.findByOwnerId(owner.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a business account");
        }

        if (businessAccountRepository.existsByCompanyName(request.getCompanyName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Company name already exists: " + request.getCompanyName());
        }

        BusinessAccount account = BusinessAccount.builder()
                .companyName(request.getCompanyName())
                .gstNumber(request.getGstNumber())
                .contactPerson(request.getContactPerson())
                .contactPhone(request.getContactPhone())
                .billingAddress(request.getBillingAddress())
                .owner(owner)
                .build();

        return toResponse(businessAccountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessAccountResponse getMyAccount() {
        User owner = getBusinessOwner();
        return toResponse(findByOwner(owner));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessAccountResponse> getAllAccounts() {
        User actor = currentUserService.getCurrentUser();
        if (Role.valueOf(actor.getRole()) != Role.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only administrators can view all business accounts");
        }

        return businessAccountRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BusinessAccountResponse updateMyAccount(BusinessAccountRequest request) {
        User owner = getBusinessOwner();
        BusinessAccount account = findByOwner(owner);

        if (!account.getCompanyName().equals(request.getCompanyName())
                && businessAccountRepository.existsByCompanyName(request.getCompanyName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Company name already exists: " + request.getCompanyName());
        }

        account.setCompanyName(request.getCompanyName());
        account.setGstNumber(request.getGstNumber());
        account.setContactPerson(request.getContactPerson());
        account.setContactPhone(request.getContactPhone());
        account.setBillingAddress(request.getBillingAddress());

        return toResponse(businessAccountRepository.save(account));
    }

    private User getBusinessOwner() {
        User user = currentUserService.getCurrentUser();
        Role role = Role.valueOf(user.getRole());
        if (role != Role.BUSINESS_CLIENT && role != Role.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only business clients and administrators can manage business accounts");
        }
        return user;
    }

    private BusinessAccount findByOwner(User owner) {
        return businessAccountRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Business account not found for the current user"));
    }

    private BusinessAccountResponse toResponse(BusinessAccount account) {
        return BusinessAccountResponse.builder()
                .id(account.getId())
                .companyName(account.getCompanyName())
                .gstNumber(account.getGstNumber())
                .contactPerson(account.getContactPerson())
                .contactPhone(account.getContactPhone())
                .billingAddress(account.getBillingAddress())
                .ownerId(account.getOwner().getId())
                .ownerName(account.getOwner().getFullName())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
