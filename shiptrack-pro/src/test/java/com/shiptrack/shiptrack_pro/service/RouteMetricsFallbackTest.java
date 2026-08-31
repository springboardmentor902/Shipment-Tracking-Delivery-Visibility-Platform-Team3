package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.RouteMetrics;
import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.entity.DeliveryRoute;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.DeliveryRouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.impl.RouteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Route creation must keep working when the Maps provider is unavailable, and
 * must use live Maps numbers when it is available.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RouteMetricsFallbackTest {

    @Mock private DeliveryRouteRepository deliveryRouteRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShipmentService shipmentService;
    @Mock private CurrentUserService currentUserService;
    @Mock private MapsService mapsService;
    @Mock private EtaService etaService;

    @InjectMocks private RouteServiceImpl routeService;

    private User operator;

    @BeforeEach
    void setUp() {
        operator = User.builder().id(7L).fullName("Ravi Operator").build();
        operator.setRole(Role.LOGISTICS_OPERATOR.name());

        Shipment shipment = Shipment.builder()
                .id(100L)
                .trackingNumber("STP1234567890")
                .createdBy(operator)
                .assignedOperator(operator)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(operator);
        when(shipmentRepository.findById(100L)).thenReturn(Optional.of(shipment));
        when(deliveryRouteRepository.findMaxLegNumber(anyLong())).thenReturn(null);
        when(deliveryRouteRepository.existsByShipmentIdAndLegNumber(anyLong(), any()))
                .thenReturn(false);
        // save() echoes the entity back, like JPA does for a new row
        when(deliveryRouteRepository.save(any(DeliveryRoute.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private RouteRequest requestWithCoordinates() {
        RouteRequest request = new RouteRequest();
        request.setShipmentId(100L);
        request.setOriginAddress("Hyderabad, Telangana");
        request.setDestinationAddress("Bengaluru, Karnataka");
        request.setOriginLatitude(new BigDecimal("17.385044"));
        request.setOriginLongitude(new BigDecimal("78.486671"));
        request.setDestinationLatitude(new BigDecimal("12.971599"));
        request.setDestinationLongitude(new BigDecimal("77.594566"));
        return request;
    }

    @Test
    @DisplayName("Maps unavailable: the leg is still created with a straight-line estimate")
    void fallsBackToStraightLineEstimate() {
        when(mapsService.isEnabled()).thenReturn(false);
        when(mapsService.geocode(anyString())).thenReturn(Optional.empty());
        when(mapsService.routeMetrics(any(), any(), any())).thenReturn(Optional.empty());

        RouteResponse response = routeService.createRoute(requestWithCoordinates());

        assertEquals(1, response.getLegNumber());
        assertEquals(RouteMetrics.SOURCE_STRAIGHT_LINE, response.getMetricsSource());
        assertNotNull(response.getDistanceKm());
        assertTrue(response.getDistanceKm().doubleValue() > 400,
                "estimate should be in the right ballpark, got " + response.getDistanceKm());
        assertNotNull(response.getExpectedDurationMinutes());
        // no provider means no traffic information at all
        assertNull(response.getDurationInTrafficMinutes());
        assertNull(response.getTrafficCondition());
    }

    @Test
    @DisplayName("Maps available: distance, duration and traffic come from the provider")
    void usesLiveMapsMetrics() {
        when(mapsService.isEnabled()).thenReturn(true);
        when(mapsService.routeMetrics(any(), any(), any())).thenReturn(Optional.of(new RouteMetrics(
                new BigDecimal("574.30"), 510, 640, "HEAVY", RouteMetrics.SOURCE_LIVE_MAPS)));

        RouteResponse response = routeService.createRoute(requestWithCoordinates());

        assertEquals(new BigDecimal("574.30"), response.getDistanceKm());
        assertEquals(510, response.getExpectedDurationMinutes());
        assertEquals(640, response.getDurationInTrafficMinutes());
        assertEquals("HEAVY", response.getTrafficCondition());
        assertEquals(RouteMetrics.SOURCE_LIVE_MAPS, response.getMetricsSource());
    }

    @Test
    @DisplayName("addresses are geocoded when the operator gives no coordinates")
    void geocodesAddressesWhenCoordinatesAreMissing() {
        when(mapsService.isEnabled()).thenReturn(true);
        when(mapsService.geocode("Hyderabad, Telangana")).thenReturn(Optional.of(
                new com.shiptrack.shiptrack_pro.dto.GeoPoint(
                        new BigDecimal("17.385044"), new BigDecimal("78.486671"), "Hyderabad")));
        when(mapsService.geocode("Bengaluru, Karnataka")).thenReturn(Optional.of(
                new com.shiptrack.shiptrack_pro.dto.GeoPoint(
                        new BigDecimal("12.971599"), new BigDecimal("77.594566"), "Bengaluru")));
        when(mapsService.routeMetrics(any(), any(), any())).thenReturn(Optional.empty());

        RouteRequest request = new RouteRequest();
        request.setShipmentId(100L);
        request.setOriginAddress("Hyderabad, Telangana");
        request.setDestinationAddress("Bengaluru, Karnataka");

        RouteResponse response = routeService.createRoute(request);

        assertEquals(new BigDecimal("17.385044"), response.getOriginLatitude());
        assertEquals(new BigDecimal("77.594566"), response.getDestinationLongitude());
    }
}
