package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.LocationUpdateRequest;
import com.shiptrack.shiptrack_pro.entity.DeliveryRoute;
import com.shiptrack.shiptrack_pro.entity.RouteLegStatus;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.DeliveryRouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.impl.TrackingServiceImpl;
import com.shiptrack.shiptrack_pro.service.LiveTrackingPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Only the operator responsible for a shipment (or an admin) may write tracking
 * data, and a valid ping must move the route leg's last known position.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrackingAuthorizationTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private TrackingEventRepository trackingEventRepository;
    @Mock private DeliveryRouteRepository deliveryRouteRepository;
    @Mock private ShipmentService shipmentService;
    @Mock private CurrentUserService currentUserService;
    @Mock private MapsService mapsService;
    @Mock private LiveTrackingPublisher liveTrackingPublisher;

    @InjectMocks private TrackingServiceImpl trackingService;

    private User assignedOperator;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        assignedOperator = user(5L, Role.LOGISTICS_OPERATOR, "Ravi Operator");

        shipment = Shipment.builder()
                .id(42L)
                .trackingNumber("STP9876543210")
                .createdBy(assignedOperator)
                .assignedOperator(assignedOperator)
                .status(ShipmentStatus.IN_TRANSIT)
                .build();

        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipment));
        when(trackingEventRepository.save(any(TrackingEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRouteRepository.save(any(DeliveryRoute.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryRouteRepository.findByShipmentIdOrderByLegNumberAsc(anyLong()))
                .thenReturn(List.of());
    }

    private User user(Long id, Role role, String name) {
        User user = User.builder().id(id).fullName(name).build();
        user.setRole(role.name());
        return user;
    }

    private LocationUpdateRequest ping() {
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.setShipmentId(42L);
        request.setLatitude(new BigDecimal("17.412300"));
        request.setLongitude(new BigDecimal("78.501200"));
        request.setLocation("Uppal, Hyderabad");
        return request;
    }

    @Test
    @DisplayName("a customer cannot record a location")
    void customerIsRejected() {
        when(currentUserService.getCurrentUser()).thenReturn(user(9L, Role.CUSTOMER, "Jaya Customer"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> trackingService.recordLocation(ping()));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(trackingEventRepository, never()).save(any());
        verify(liveTrackingPublisher, never()).publishLocation(any(), any());
    }

    @Test
    @DisplayName("an operator who is not on this shipment cannot record a location")
    void unassignedOperatorIsRejected() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(11L, Role.LOGISTICS_OPERATOR, "Other Operator"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> trackingService.recordLocation(ping()));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(trackingEventRepository, never()).save(any());
        verify(liveTrackingPublisher, never()).publishLocation(any(), any());
    }

    @Test
    @DisplayName("the assigned operator records a location and the leg follows the driver")
    void assignedOperatorUpdatesLeg() {
        when(currentUserService.getCurrentUser()).thenReturn(assignedOperator);

        DeliveryRoute leg = DeliveryRoute.builder()
                .id(1L)
                .shipment(shipment)
                .legNumber(1)
                .driver(assignedOperator)
                .originAddress("Hyderabad")
                .destinationAddress("Bengaluru")
                .status(RouteLegStatus.PLANNED)
                .build();
        when(deliveryRouteRepository.findByShipmentIdOrderByLegNumberAsc(42L)).thenReturn(List.of(leg));

        var response = trackingService.recordLocation(ping());

        assertEquals("Uppal, Hyderabad", response.getLocation());
        assertEquals(new BigDecimal("17.412300"), leg.getLastKnownLatitude());
        assertEquals(new BigDecimal("78.501200"), leg.getLastKnownLongitude());
        assertNotNull(leg.getLastLocationAt());
        // the first ping puts the leg in progress
        assertEquals(RouteLegStatus.ACTIVE, leg.getStatus());
        verify(deliveryRouteRepository).save(leg);
        // watchers of this shipment are pushed the new position
        verify(liveTrackingPublisher).publishLocation(shipment, response);
    }

    @Test
    @DisplayName("a driver of one leg may record even if another operator owns the shipment")
    void legDriverIsAllowed() {
        User legDriver = user(21L, Role.LOGISTICS_OPERATOR, "Leg Driver");
        when(currentUserService.getCurrentUser()).thenReturn(legDriver);

        DeliveryRoute leg = DeliveryRoute.builder()
                .id(2L)
                .shipment(shipment)
                .legNumber(2)
                .driver(legDriver)
                .originAddress("Kurnool")
                .destinationAddress("Bengaluru")
                .status(RouteLegStatus.ACTIVE)
                .build();
        when(deliveryRouteRepository.findByShipmentIdOrderByLegNumberAsc(42L)).thenReturn(List.of(leg));

        var response = trackingService.recordLocation(ping());

        assertNotNull(response);
        assertEquals(new BigDecimal("17.412300"), leg.getLastKnownLatitude());
    }

    @Test
    @DisplayName("a delivered shipment rejects further tracking updates")
    void deliveredShipmentIsClosed() {
        shipment.setStatus(ShipmentStatus.DELIVERED);
        when(currentUserService.getCurrentUser()).thenReturn(assignedOperator);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> trackingService.recordLocation(ping()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}
