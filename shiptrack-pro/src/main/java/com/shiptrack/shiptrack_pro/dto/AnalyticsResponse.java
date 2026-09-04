package com.shiptrack.shiptrack_pro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    // ==========================================
    // COMMON SHIPMENT ANALYTICS
    // ==========================================

    private long totalShipments;

    private long activeShipments;

    private long deliveredShipments;

    private long pendingShipments;

    private long inProgressShipments;

    private long cancelledShipments;

    // ==========================================
    // STATUS BREAKDOWN
    // ==========================================

    private Map<String, Long> statusBreakdown;

    // ==========================================
    // TRACKING INSIGHTS
    // ==========================================

    private long delayedShipments;

    private long onTimeShipments;

    // ==========================================
    // DELIVERY PERFORMANCE
    // ==========================================

    private double deliveryRate;

    private double delayRate;

    private double averageDelayMinutes;

    // ==========================================
    // USER INFORMATION
    // ==========================================

    private Long userId;

    private String userName;

    private String userRole;

    // ==========================================
    // BUSINESS CLIENT ANALYTICS
    // ==========================================

    private long customerActivity;

    // ==========================================
    // ROUTE PERFORMANCE
    // ==========================================

    private long totalRoutes;

    private double totalDistanceKm;

    private double averageDistanceKm;

    private double averageEstimatedTimeMinutes;

    private double averageActualTimeMinutes;

    private long onTimeRoutes;

    private long delayedRoutes;

    // ==========================================
    // TRAFFIC INFORMATION
    // ==========================================

    private Map<String, Long> trafficConditionBreakdown;

    // ==========================================
    // ADMIN USER SUMMARY
    // ==========================================

    private long totalUsers;

    private long totalCustomers;

    private long totalBusinessClients;

    private long totalOperators;

    private long totalDrivers;

    private long totalAdministrators;

    // ==========================================
    // SYSTEM MONITORING
    // ==========================================

    private long activeUsers;

    private long activeRoutes;

    // ==========================================
    // REPORT MANAGEMENT
    // ==========================================

    private long totalReports;
}