package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.BusinessAccountRequest;
import com.shiptrack.shiptrack_pro.dto.BusinessAccountResponse;
import com.shiptrack.shiptrack_pro.service.BusinessAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business-accounts")
@RequiredArgsConstructor
public class BusinessAccountController {

    private final BusinessAccountService businessAccountService;

    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_CLIENT', 'ADMINISTRATOR')")
    public ResponseEntity<BusinessAccountResponse> createMyAccount(
            @Valid @RequestBody BusinessAccountRequest request) {
        return new ResponseEntity<>(businessAccountService.createMyAccount(request), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('BUSINESS_CLIENT', 'ADMINISTRATOR')")
    public ResponseEntity<BusinessAccountResponse> getMyAccount() {
        return ResponseEntity.ok(businessAccountService.getMyAccount());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<BusinessAccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(businessAccountService.getAllAccounts());
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('BUSINESS_CLIENT', 'ADMINISTRATOR')")
    public ResponseEntity<BusinessAccountResponse> updateMyAccount(
            @Valid @RequestBody BusinessAccountRequest request) {
        return ResponseEntity.ok(businessAccountService.updateMyAccount(request));
    }
}
