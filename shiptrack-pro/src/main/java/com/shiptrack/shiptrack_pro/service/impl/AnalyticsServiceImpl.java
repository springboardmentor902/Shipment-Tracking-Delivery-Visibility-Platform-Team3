package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.config.RedisConfig;
import com.shiptrack.shiptrack_pro.dto.analytics.AdminAnalyticsResponse;
import com.shiptrack.shiptrack_pro.dto.analytics.BusinessAnalyticsResponse;
import com.shiptrack.shiptrack_pro.dto.analytics.CustomerAnalyticsResponse;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.*;
import com.shiptrack.shiptrack_pro.service.AnalyticsService;
import com.shiptrack.shiptrack_pro.service.support.DeliveryMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final ProofOfDeliveryRepository podRepository;

    private static final int TOP_RECEIVERS_LIMIT = 5;

    // =========================================================
    // CUSTOMER ANALYTICS
    // GET /api/analytics/customer
    // =========================================================
    @Override
    @Cacheable(value = RedisConfig.CUSTOMER_ANALYTICS_CACHE, key = "#customerEmail")
    public CustomerAnalyticsResponse getCustomerAnalytics(String customerEmail) {

        User customer = getUserOrThrow(customerEmail);
        List<Shipment> shipments = shipmentRepository.findByCustomer(customer);

        long active = shipments.stream().filter(DeliveryMetrics::isActive).count();

        Map<String, Long> statusBreakdown = DeliveryMetrics.statusBreakdown(shipments);

        List<CustomerAnalyticsResponse.ShipmentHistoryItem> history = shipments.stream()
                .sorted(Comparator.comparing(Shipment::getCreatedAt).reversed())
                .map(s -> CustomerAnalyticsResponse.ShipmentHistoryItem.builder()
                        .trackingNumber(s.getTrackingNumber())
                        .status(s.getStatus())
                        .receiverName(s.getReceiverName())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();

        CustomerAnalyticsResponse.TrackingInsights insights = buildCustomerInsights(shipments);

        return CustomerAnalyticsResponse.builder()
                .activeShipmentCount(active)
                .totalShipmentCount(shipments.size())
                .statusBreakdown(statusBreakdown)
                .shipmentHistory(history)
                .trackingInsights(insights)
                .build();
    }

    private CustomerAnalyticsResponse.TrackingInsights buildCustomerInsights(List<Shipment> shipments) {
        if (shipments.isEmpty()) {
            return CustomerAnalyticsResponse.TrackingInsights.builder().build();
        }

        Shipment mostRecent = shipments.stream()
                .max(Comparator.comparing(Shipment::getCreatedAt))
                .orElseThrow();

        List<Shipment> delivered = shipments.stream()
                .filter(DeliveryMetrics::isDelivered)
                .toList();

        Double onTimeRate = computeOnTimeRate(delivered);
        Double avgHours = average(delivered.stream()
                .map(DeliveryMetrics::deliveryDurationHours)
                .filter(java.util.Objects::nonNull)
                .toList());

        return CustomerAnalyticsResponse.TrackingInsights.builder()
                .mostRecentStatus(mostRecent.getStatus())
                .mostRecentTrackingNumber(mostRecent.getTrackingNumber())
                .onTimeDeliveryRatePercent(onTimeRate)
                .averageDeliveryTimeHours(avgHours)
                .build();
    }

    // =========================================================
    // BUSINESS CLIENT ANALYTICS
    // GET /api/analytics/business
    // =========================================================
    @Override
    @Cacheable(value = RedisConfig.BUSINESS_ANALYTICS_CACHE, key = "#businessEmail")
    public BusinessAnalyticsResponse getBusinessAnalytics(String businessEmail) {

        User business = getUserOrThrow(businessEmail);
        List<Shipment> shipments = shipmentRepository.findByCustomer(business);

        long active = shipments.stream().filter(DeliveryMetrics::isActive).count();
        List<Shipment> delivered = shipments.stream().filter(DeliveryMetrics::isDelivered).toList();

        Map<String, Long> statusBreakdown = DeliveryMetrics.statusBreakdown(shipments);

        // Delivery performance
        Double onTimeRate = computeOnTimeRate(delivered);
        Double avgDeliveryHours = average(delivered.stream()
                .map(DeliveryMetrics::deliveryDurationHours)
                .filter(java.util.Objects::nonNull)
                .toList());

        // Delay analysis - based on route actual vs estimated time
        List<Route> routes = shipments.stream()
                .map(s -> routeRepository.findByShipmentId(s.getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        long delayedCount = routes.stream()
                .map(DeliveryMetrics::isOnTime)
                .filter(onTime -> Boolean.FALSE.equals(onTime))
                .count();

        Double avgDelayMinutes = average(routes.stream()
                .map(DeliveryMetrics::delayMinutes)
                .filter(java.util.Objects::nonNull)
                .map(Integer::doubleValue)
                .toList());

        // Customer activity (activity of the receivers this business ships to)
        Map<String, Long> receiverCounts = shipments.stream()
                .collect(Collectors.groupingBy(Shipment::getReceiverName, Collectors.counting()));

        Map<String, Long> topReceivers = receiverCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_RECEIVERS_LIMIT)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, java.util.LinkedHashMap::new));

        // Logistics overview
        double totalDistance = routes.stream()
                .map(Route::getDistanceKm)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        long routesWithDriver = routes.stream().filter(r -> r.getDriver() != null).count();

        return BusinessAnalyticsResponse.builder()
                .totalShipments(shipments.size())
                .activeShipments(active)
                .deliveredShipments(delivered.size())
                .statusBreakdown(statusBreakdown)
                .deliveryPerformance(BusinessAnalyticsResponse.DeliveryPerformance.builder()
                        .onTimeDeliveryRatePercent(onTimeRate)
                        .averageDeliveryTimeHours(avgDeliveryHours)
                        .build())
                .delayAnalysis(BusinessAnalyticsResponse.DelayAnalysis.builder()
                        .delayedShipmentCount(delayedCount)
                        .averageDelayMinutes(avgDelayMinutes)
                        .build())
                .customerActivity(BusinessAnalyticsResponse.CustomerActivity.builder()
                        .distinctReceiverCount(receiverCounts.size())
                        .topReceivers(topReceivers)
                        .build())
                .logisticsOverview(BusinessAnalyticsResponse.LogisticsOverview.builder()
                        .shipmentCountByStatus(statusBreakdown)
                        .totalDistanceCoveredKm(totalDistance)
                        .routesWithDriverAssigned(routesWithDriver)
                        .totalRoutes(routes.size())
                        .build())
                .build();
    }

    // =========================================================
    // ADMIN (PLATFORM-WIDE) ANALYTICS
    // GET /api/analytics/admin
    // =========================================================
    @Override
    @Cacheable(value = RedisConfig.ADMIN_ANALYTICS_CACHE)
    public AdminAnalyticsResponse getAdminAnalytics() {

        List<Shipment> allShipments = shipmentRepository.findAll();
        List<Route> allRoutes = routeRepository.findAll();

        // ---- User summary ----
        long totalUsers = userRepository.count();
        Map<String, Long> usersByRole = userRepository.findAll().stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        long newUsers30d = userRepository.countByCreatedAtAfter(
                java.time.LocalDateTime.now().minusDays(30));

        // ---- Shipment monitoring ----
        Map<String, Long> shipmentsByStatus = DeliveryMetrics.statusBreakdown(allShipments);
        long createdLast7Days = allShipments.stream()
                .filter(s -> DeliveryMetrics.isRecentlyCreated(s, 7))
                .count();

        // ---- Delivery analytics ----
        List<Shipment> delivered = allShipments.stream().filter(DeliveryMetrics::isDelivered).toList();
        long rejected = allShipments.stream().filter(DeliveryMetrics::isRejected).count();
        Double onTimeRate = computeOnTimeRate(delivered);
        Double avgDeliveryHours = average(delivered.stream()
                .map(DeliveryMetrics::deliveryDurationHours)
                .filter(java.util.Objects::nonNull)
                .toList());

        // ---- Route performance ----
        Double avgDistance = average(allRoutes.stream()
                .map(Route::getDistanceKm).filter(java.util.Objects::nonNull).toList());
        Double avgEstimated = average(allRoutes.stream()
                .map(Route::getEstimatedTimeMinutes).filter(java.util.Objects::nonNull)
                .map(Integer::doubleValue).toList());
        Double avgActual = average(allRoutes.stream()
                .map(Route::getActualTimeMinutes).filter(java.util.Objects::nonNull)
                .map(Integer::doubleValue).toList());
        Double avgDelay = average(allRoutes.stream()
                .map(DeliveryMetrics::delayMinutes).filter(java.util.Objects::nonNull)
                .map(Integer::doubleValue).toList());

        // ---- System monitoring ----
        long pendingPod = podRepository.findByStatusOrderBySubmittedAtAsc("PENDING").size();
        long activeDrivers = allRoutes.stream()
                .map(Route::getDriver)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .distinct()
                .count();
        long routesInProgress = allRoutes.stream()
                .filter(r -> r.getShipment() != null && DeliveryMetrics.isActive(r.getShipment()))
                .count();

        // ---- Reports management (what's available to generate right now) ----
        AdminAnalyticsResponse.ReportsManagement reportsManagement =
                AdminAnalyticsResponse.ReportsManagement.builder()
                        .availableReports(List.of(
                                report("shipments", "Shipment Report", allShipments.size()),
                                report("delivery", "Delivery Report", delivered.size()),
                                report("route-performance", "Route Performance Report", allRoutes.size()),
                                report("delay-analysis", "Delay Analysis Report", allRoutes.size())
                        ))
                        .build();

        return AdminAnalyticsResponse.builder()
                .userSummary(AdminAnalyticsResponse.UserSummary.builder()
                        .totalUsers(totalUsers)
                        .usersByRole(usersByRole)
                        .newUsersLast30Days(newUsers30d)
                        .build())
                .shipmentMonitoring(AdminAnalyticsResponse.ShipmentMonitoring.builder()
                        .totalShipments(allShipments.size())
                        .shipmentsByStatus(shipmentsByStatus)
                        .shipmentsCreatedLast7Days(createdLast7Days)
                        .build())
                .deliveryAnalytics(AdminAnalyticsResponse.DeliveryAnalytics.builder()
                        .totalDelivered(delivered.size())
                        .totalRejected(rejected)
                        .onTimeDeliveryRatePercent(onTimeRate)
                        .averageDeliveryTimeHours(avgDeliveryHours)
                        .build())
                .routePerformance(AdminAnalyticsResponse.RoutePerformance.builder()
                        .totalRoutes(allRoutes.size())
                        .averageDistanceKm(avgDistance)
                        .averageEstimatedTimeMinutes(avgEstimated)
                        .averageActualTimeMinutes(avgActual)
                        .averageDelayMinutes(avgDelay)
                        .build())
                .systemMonitoring(AdminAnalyticsResponse.SystemMonitoring.builder()
                        .pendingPodVerifications(pendingPod)
                        .activeDriversAssigned(activeDrivers)
                        .routesInProgress(routesInProgress)
                        .build())
                .reportsManagement(reportsManagement)
                .build();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private AdminAnalyticsResponse.ReportsManagement.AvailableReport report(
            String type, String displayName, long count) {
        return AdminAnalyticsResponse.ReportsManagement.AvailableReport.builder()
                .reportType(type)
                .displayName(displayName)
                .recordCount(count)
                .build();
    }

    private Double computeOnTimeRate(List<Shipment> deliveredShipments) {
        List<Boolean> known = deliveredShipments.stream()
                .map(s -> routeRepository.findByShipmentId(s.getId()).orElse(null))
                .map(DeliveryMetrics::isOnTime)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (known.isEmpty()) return null;

        long onTimeCount = known.stream().filter(Boolean::booleanValue).count();
        return (onTimeCount * 100.0) / known.size();
    }

    private Double average(List<Double> values) {
        if (values.isEmpty()) return null;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found: " + email
                ));
    }
}
