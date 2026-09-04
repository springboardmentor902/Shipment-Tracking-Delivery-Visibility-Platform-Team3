package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.AnalyticsResponse;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    private final UserRepository userRepository;

    // ==========================================
    // CUSTOMER ANALYTICS
    // ==========================================

    @GetMapping("/customer")
    public ResponseEntity<AnalyticsResponse> getCustomerAnalytics(
            Authentication authentication) {

        // ==========================================
        // DEBUG
        // ==========================================

        System.out.println(
                "========== ANALYTICS DEBUG =========="
        );

        System.out.println(
                "Authenticated: "
                        + authentication.isAuthenticated()
        );

        System.out.println(
                "Username: "
                        + authentication.getName()
        );

        System.out.println(
                "Authorities: "
                        + authentication.getAuthorities()
        );

        System.out.println(
                "====================================="
        );

        // ==========================================
        // Get Logged-in User
        // ==========================================

        User user =
                getAuthenticatedUser(authentication);

        // ==========================================
        // Get Customer Analytics
        // ==========================================

        AnalyticsResponse response =
                analyticsService.getCustomerAnalytics(user);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // BUSINESS CLIENT ANALYTICS
    // ==========================================

    @GetMapping("/business")
    public ResponseEntity<AnalyticsResponse> getBusinessAnalytics(
            Authentication authentication) {

        User user =
                getAuthenticatedUser(authentication);

        AnalyticsResponse response =
                analyticsService.getBusinessAnalytics(user);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // ADMIN ANALYTICS
    // ==========================================

    @GetMapping("/admin")
    public ResponseEntity<AnalyticsResponse> getAdminAnalytics() {

        AnalyticsResponse response =
                analyticsService.getAdminAnalytics();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Get Authenticated User
    // ==========================================

    private User getAuthenticatedUser(
            Authentication authentication) {

        String email =
                authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found: "
                                        + email
                        )
                );
    }
}