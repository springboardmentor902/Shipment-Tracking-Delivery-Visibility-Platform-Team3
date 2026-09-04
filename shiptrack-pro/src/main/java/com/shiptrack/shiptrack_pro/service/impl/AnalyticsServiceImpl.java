package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.AnalyticsResponse;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;

    // ==========================================
    // CUSTOMER ANALYTICS
    // ==========================================

    @Override
    public AnalyticsResponse getCustomerAnalytics(User user) {

        List<Shipment> shipments =
                shipmentRepository.findByCustomer(user);

        return buildShipmentAnalytics(shipments, user);
    }

    // ==========================================
    // BUSINESS CLIENT ANALYTICS
    // ==========================================

    @Override
    public AnalyticsResponse getBusinessAnalytics(User user) {

        /*
         * In your current project, Shipment.customer represents
         * the CUSTOMER or BUSINESS_CLIENT who created the shipment.
         *
         * Therefore, this correctly restricts the data to the
         * logged-in business client's own shipments.
         */
        List<Shipment> shipments =
                shipmentRepository.findByCustomer(user);

        return buildShipmentAnalytics(shipments, user);
    }

    // ==========================================
    // ADMIN ANALYTICS
    // ==========================================

    @Override
    public AnalyticsResponse getAdminAnalytics() {

        // Get all platform shipments
        List<Shipment> shipments =
                shipmentRepository.findAll();

        // ------------------------------------------
        // User Summary
        // ------------------------------------------

        List<User> users =
                userRepository.findAll();

        long totalUsers = users.size();

        long totalCustomers =
                users.stream()
                        .filter(u ->
                                "CUSTOMER".equalsIgnoreCase(
                                        u.getRole()
                                ))
                        .count();

        long totalBusinessClients =
                users.stream()
                        .filter(u ->
                                "BUSINESS_CLIENT".equalsIgnoreCase(
                                        u.getRole()
                                ))
                        .count();

        long totalOperators =
                users.stream()
                        .filter(u ->
                                "LOGISTICS_OPERATOR".equalsIgnoreCase(
                                        u.getRole()
                                ))
                        .count();

        long totalDrivers =
                users.stream()
                        .filter(u ->
                                "DRIVER".equalsIgnoreCase(
                                        u.getRole()
                                ))
                        .count();

        long totalAdministrators =
                users.stream()
                        .filter(u ->
                                "ADMINISTRATOR".equalsIgnoreCase(
                                        u.getRole()
                                ))
                        .count();

        // ------------------------------------------
        // Build platform shipment analytics
        // ------------------------------------------

        AnalyticsResponse response =
                buildShipmentAnalytics(
                        shipments,
                        null
                );

        // ------------------------------------------
        // Admin User Summary
        // ------------------------------------------

        response.setTotalUsers(totalUsers);
        response.setTotalCustomers(totalCustomers);
        response.setTotalBusinessClients(totalBusinessClients);
        response.setTotalOperators(totalOperators);
        response.setTotalDrivers(totalDrivers);
        response.setTotalAdministrators(totalAdministrators);

        return response;
    }

    // ==========================================
    // BUILD SHIPMENT ANALYTICS
    // ==========================================

    private AnalyticsResponse buildShipmentAnalytics(
            List<Shipment> shipments,
            User user) {

        long totalShipments =
                shipments.size();

        long activeShipments =
                shipments.stream()
                        .filter(this::isActive)
                        .count();

        long deliveredShipments =
                countByStatus(
                        shipments,
                        "DELIVERED"
                );

        long pendingShipments =
                countByStatus(
                        shipments,
                        "PENDING"
                );

        long inProgressShipments =
                countByStatus(
                        shipments,
                        "IN_PROGRESS"
                );

        long cancelledShipments =
                countByStatus(
                        shipments,
                        "CANCELLED"
                );

        // ------------------------------------------
        // Status Breakdown
        // ------------------------------------------

        Map<String, Long> statusBreakdown =
                buildStatusBreakdown(shipments);

        // ------------------------------------------
        // Route Analytics
        // ------------------------------------------

        RouteAnalytics routeAnalytics =
                buildRouteAnalytics(shipments);

        // ------------------------------------------
        // Delivery Rate
        // ------------------------------------------

        double deliveryRate = 0.0;

        if (totalShipments > 0) {
            deliveryRate =
                    ((double) deliveredShipments /
                            totalShipments) * 100.0;
        }

        // ------------------------------------------
        // Delay Rate
        // ------------------------------------------

        double delayRate = 0.0;

        if (totalShipments > 0) {
            delayRate =
                    ((double) routeAnalytics.delayedRoutes /
                            totalShipments) * 100.0;
        }

        // ------------------------------------------
        // Build Response
        // ------------------------------------------

        AnalyticsResponse.AnalyticsResponseBuilder builder =
                AnalyticsResponse.builder()

                        .totalShipments(totalShipments)

                        .activeShipments(activeShipments)

                        .deliveredShipments(deliveredShipments)

                        .pendingShipments(pendingShipments)

                        .inProgressShipments(
                                inProgressShipments
                        )

                        .cancelledShipments(
                                cancelledShipments
                        )

                        .statusBreakdown(
                                statusBreakdown
                        );

        // ------------------------------------------
        // Add User Information
        // ------------------------------------------

        if (user != null) {

            builder
                    .userId(user.getId())
                    .userName(user.getFullName())
                    .userRole(user.getRole());
        }

        /*
         * IMPORTANT:
         *
         * The AnalyticsResponse you currently have does NOT
         * contain fields for route distance, delay rate,
         * delivery rate, etc.
         *
         * Therefore we are NOT calling setters for fields
         * that don't exist in your current DTO.
         *
         * We calculate the values internally here so the
         * code is ready for the next DTO/report stage.
         */

        return builder.build();
    }

    // ==========================================
    // ROUTE ANALYTICS
    // ==========================================

    private RouteAnalytics buildRouteAnalytics(
            List<Shipment> shipments) {

        int totalRoutes = 0;

        double totalDistance = 0.0;

        double totalEstimatedTime = 0.0;

        double totalActualTime = 0.0;

        int estimatedTimeCount = 0;

        int actualTimeCount = 0;

        int onTimeRoutes = 0;

        int delayedRoutes = 0;

        Map<String, Long> trafficBreakdown =
                new LinkedHashMap<>();

        for (Shipment shipment : shipments) {

            try {

                Route route =
                        routeRepository
                                .findByShipmentId(
                                        shipment.getId()
                                )
                                .orElse(null);

                if (route == null) {
                    continue;
                }

                totalRoutes++;

                // ----------------------------------
                // Distance
                // ----------------------------------

                if (route.getDistanceKm() != null) {

                    totalDistance +=
                            route.getDistanceKm();
                }

                // ----------------------------------
                // Estimated Time
                // ----------------------------------

                if (route.getEstimatedTimeMinutes()
                        != null) {

                    totalEstimatedTime +=
                            route.getEstimatedTimeMinutes();

                    estimatedTimeCount++;
                }

                // ----------------------------------
                // Actual Time
                // ----------------------------------

                if (route.getActualTimeMinutes()
                        != null) {

                    totalActualTime +=
                            route.getActualTimeMinutes();

                    actualTimeCount++;
                }

                // ----------------------------------
                // Delay Analysis
                // ----------------------------------

                if (route.getEstimatedTimeMinutes() != null
                        && route.getActualTimeMinutes() != null) {

                    if (route.getActualTimeMinutes()
                            > route.getEstimatedTimeMinutes()) {

                        delayedRoutes++;

                    } else {

                        onTimeRoutes++;
                    }
                }

                // ----------------------------------
                // Traffic Condition
                // ----------------------------------

                String traffic =
                        route.getTrafficCondition();

                if (traffic == null
                        || traffic.isBlank()) {

                    traffic = "UNKNOWN";
                }

                trafficBreakdown.put(
                        traffic,
                        trafficBreakdown.getOrDefault(
                                traffic,
                                0L
                        ) + 1
                );

            } catch (Exception e) {

                /*
                 * If one route has a problem, don't allow
                 * one bad route to break the entire
                 * analytics request.
                 */
            }
        }

        double averageEstimatedTime = 0.0;

        if (estimatedTimeCount > 0) {

            averageEstimatedTime =
                    totalEstimatedTime /
                            estimatedTimeCount;
        }

        double averageActualTime = 0.0;

        if (actualTimeCount > 0) {

            averageActualTime =
                    totalActualTime /
                            actualTimeCount;
        }

        double averageDistance = 0.0;

        if (totalRoutes > 0) {

            averageDistance =
                    totalDistance /
                            totalRoutes;
        }

        return new RouteAnalytics(
                totalRoutes,
                totalDistance,
                averageDistance,
                averageEstimatedTime,
                averageActualTime,
                onTimeRoutes,
                delayedRoutes,
                trafficBreakdown
        );
    }

    // ==========================================
    // ACTIVE SHIPMENT
    // ==========================================

    private boolean isActive(
            Shipment shipment) {

        String status =
                shipment.getStatus();

        if (status == null) {
            return false;
        }

        return !status.equalsIgnoreCase("DELIVERED")
                && !status.equalsIgnoreCase("CANCELLED");
    }

    // ==========================================
    // COUNT BY STATUS
    // ==========================================

    private long countByStatus(
            List<Shipment> shipments,
            String status) {

        return shipments.stream()
                .filter(shipment ->
                        shipment.getStatus() != null
                                && shipment.getStatus()
                                .equalsIgnoreCase(status)
                )
                .count();
    }

    // ==========================================
    // STATUS BREAKDOWN
    // ==========================================

    private Map<String, Long> buildStatusBreakdown(
            List<Shipment> shipments) {

        Map<String, Long> breakdown =
                new LinkedHashMap<>();

        for (Shipment shipment : shipments) {

            String status =
                    shipment.getStatus();

            if (status == null
                    || status.isBlank()) {

                status = "UNKNOWN";
            }

            breakdown.put(
                    status,
                    breakdown.getOrDefault(
                            status,
                            0L
                    ) + 1
            );
        }

        return breakdown;
    }

    // ==========================================
    // INTERNAL ROUTE ANALYTICS CLASS
    // ==========================================

    private static class RouteAnalytics {

        private final int totalRoutes;

        private final double totalDistance;

        private final double averageDistance;

        private final double averageEstimatedTime;

        private final double averageActualTime;

        private final int onTimeRoutes;

        private final int delayedRoutes;

        private final Map<String, Long>
                trafficBreakdown;

        public RouteAnalytics(
                int totalRoutes,
                double totalDistance,
                double averageDistance,
                double averageEstimatedTime,
                double averageActualTime,
                int onTimeRoutes,
                int delayedRoutes,
                Map<String, Long> trafficBreakdown) {

            this.totalRoutes =
                    totalRoutes;

            this.totalDistance =
                    totalDistance;

            this.averageDistance =
                    averageDistance;

            this.averageEstimatedTime =
                    averageEstimatedTime;

            this.averageActualTime =
                    averageActualTime;

            this.onTimeRoutes =
                    onTimeRoutes;

            this.delayedRoutes =
                    delayedRoutes;

            this.trafficBreakdown =
                    trafficBreakdown;
        }
    }
}