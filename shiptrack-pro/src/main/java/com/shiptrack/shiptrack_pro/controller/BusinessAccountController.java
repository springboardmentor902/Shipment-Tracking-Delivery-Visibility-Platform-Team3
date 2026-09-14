package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.BusinessAccountRequest;
import com.shiptrack.shiptrack_pro.service.BusinessAccountService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business-accounts")
@RequiredArgsConstructor
public class BusinessAccountController {

    private final BusinessAccountService businessAccountService;

    @GetMapping
    public ResponseEntity<?> listBusinessAccounts() {
        return ResponseEntity.ok(
                businessAccountService.findAll()
        );
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyBusinessAccount() {
        return ResponseEntity.ok(
                businessAccountService.getMyAccount()
        );
    }

    @PostMapping
    public ResponseEntity<?> createBusinessAccount(
            @RequestBody BusinessAccountRequest request
    ) {
        return ResponseEntity.ok(
                businessAccountService.create(request)
        );
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyBusinessAccount(
            @RequestBody BusinessAccountRequest request
    ) {
        return ResponseEntity.ok(
                businessAccountService.updateMyAccount(request)
        );
    }
}