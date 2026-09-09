package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.analytics.AdminAnalyticsResponse;
import com.shiptrack.shiptrack_pro.dto.analytics.BusinessAnalyticsResponse;
import com.shiptrack.shiptrack_pro.dto.analytics.CustomerAnalyticsResponse;
import com.shiptrack.shiptrack_pro.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // GET /api/analytics/customer - own shipments only
    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerAnalyticsResponse> getCustomerAnalytics(
            Authentication authentication) {
        return ResponseEntity.ok(
                analyticsService.getCustomerAnalytics(authentication.getName())
        );
    }

    // GET /api/analytics/business - own business's shipments only
    @GetMapping("/business")
    @PreAuthorize("hasRole('BUSINESS_CLIENT')")
    public ResponseEntity<BusinessAnalyticsResponse> getBusinessAnalytics(
            Authentication authentication) {
        return ResponseEntity.ok(
                analyticsService.getBusinessAnalytics(authentication.getName())
        );
    }

    // GET /api/analytics/admin - platform-wide
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<AdminAnalyticsResponse> getAdminAnalytics() {
        return ResponseEntity.ok(analyticsService.getAdminAnalytics());
    }
}
