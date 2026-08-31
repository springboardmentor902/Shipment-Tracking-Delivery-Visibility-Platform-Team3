package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.EtaResponse;
import com.shiptrack.shiptrack_pro.entity.DelayRiskLevel;
import com.shiptrack.shiptrack_pro.entity.EtaPrediction;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.DeliveryRouteRepository;
import com.shiptrack.shiptrack_pro.repository.EtaPredictionRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.security.ShipmentAccessPolicy;
import com.shiptrack.shiptrack_pro.service.impl.EtaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Who may read a forecast, and how the at-risk list is scoped.
 *
 * A forecast leaks the receiver name and delivery address, so it must obey the
 * same visibility rules as the shipment itself.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EtaAccessControlTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private DeliveryRouteRepository routeRepository;
    @Mock private TrackingEventRepository trackingEventRepository;
    @Mock private EtaPredictionRepository etaPredictionRepository;
    @Mock private CurrentUserService currentUserService;

    private EtaServiceImpl etaService;

    private User owner;
    private User assignedOperator;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        // the real policy, so these tests exercise the actual rules
        ShipmentAccessPolicy accessPolicy = new ShipmentAccessPolicy();
        etaService = new EtaServiceImpl(shipmentRepository, routeRepository, trackingEventRepository,
                etaPredictionRepository, currentUserService, accessPolicy);

        owner = user(1L, Role.BUSINESS_CLIENT, "Anita Business");
        assignedOperator = user(2L, Role.LOGISTICS_OPERATOR, "Ravi Operator");

        shipment = Shipment.builder()
                .id(42L)
                .trackingNumber("STP9876543210")
                .createdBy(owner)
                .assignedOperator(assignedOperator)
                .receiverName("Kiran Receiver")
                .receiverEmail("kiran@example.com")
                .deliveryAddress("Banjara Hills, Hyderabad")
                .status(ShipmentStatus.IN_TRANSIT)
                .estimatedDeliveryDate(LocalDate.now().plusDays(2))
                .build();

        when(shipmentRepository.findById(42L)).thenReturn(Optional.of(shipment));
        when(routeRepository.findByShipmentIdOrderByLegNumberAsc(anyLong())).thenReturn(List.of());
        when(trackingEventRepository.findFirstByShipmentOrderByRecordedAtDesc(any()))
                .thenReturn(Optional.empty());
        when(etaPredictionRepository.save(any(EtaPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User user(Long id, Role role, String name) {
        User user = User.builder().id(id).fullName(name).build();
        user.setRole(role.name());
        return user;
    }

    private EtaPrediction prediction(Shipment target, int score) {
        return EtaPrediction.builder()
                .id(target.getId())
                .shipment(target)
                .predictedDeliveryAt(LocalDateTime.now().plusHours(6))
                .promisedDeliveryDate(target.getEstimatedDeliveryDate())
                .expectedDelayMinutes(120)
                .delayRiskScore(score)
                .riskLevel(DelayRiskLevel.fromScore(score))
                .confidenceScore(80)
                .factors("Running about 2 hours past the promised date.")
                .source(EtaCalculator.SOURCE_ROUTE_METRICS)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("the business client who booked the shipment can read its ETA")
    void ownerCanRead() {
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(etaPredictionRepository.findByShipmentId(42L))
                .thenReturn(Optional.of(prediction(shipment, 60)));

        EtaResponse response = etaService.getForShipment(42L);

        assertEquals(42L, response.getShipmentId());
        assertEquals("HIGH", response.getRiskLevel());
        assertEquals(List.of("Running about 2 hours past the promised date."), response.getFactors());
    }

    @Test
    @DisplayName("the assigned operator can read the ETA")
    void assignedOperatorCanRead() {
        when(currentUserService.getCurrentUser()).thenReturn(assignedOperator);
        when(etaPredictionRepository.findByShipmentId(42L))
                .thenReturn(Optional.of(prediction(shipment, 10)));

        assertEquals("LOW", etaService.getForShipment(42L).getRiskLevel());
    }

    @Test
    @DisplayName("an unrelated customer is refused")
    void strangerIsRefused() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(9L, Role.CUSTOMER, "Nosy Customer"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> etaService.getForShipment(42L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    @DisplayName("an operator who is not on this shipment is refused")
    void unassignedOperatorIsRefused() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(11L, Role.LOGISTICS_OPERATOR, "Other Operator"));

        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class,
                        () -> etaService.getForShipment(42L)).getStatusCode());
    }

    @Test
    @DisplayName("the receiver named on the shipment can read the ETA")
    void receiverCanRead() {
        User receiver = user(12L, Role.CUSTOMER, "Kiran Receiver");
        receiver.setEmail("kiran@example.com");
        when(currentUserService.getCurrentUser()).thenReturn(receiver);
        when(etaPredictionRepository.findByShipmentId(42L))
                .thenReturn(Optional.of(prediction(shipment, 30)));

        assertEquals("MEDIUM", etaService.getForShipment(42L).getRiskLevel());
    }

    @Test
    @DisplayName("a missing forecast is calculated on demand instead of failing")
    void missingForecastIsCalculated() {
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(etaPredictionRepository.findByShipmentId(42L)).thenReturn(Optional.empty());

        EtaResponse response = etaService.getForShipment(42L);

        assertNotNull(response.getPredictedDeliveryAt());
        verify(etaPredictionRepository).save(any(EtaPrediction.class));
    }

    @Test
    @DisplayName("the at-risk list hides shipments the caller may not see")
    void atRiskListIsScoped() {
        Shipment other = Shipment.builder()
                .id(99L)
                .trackingNumber("STP0000000099")
                .createdBy(user(77L, Role.BUSINESS_CLIENT, "Rival Business"))
                .status(ShipmentStatus.IN_TRANSIT)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(etaPredictionRepository
                .findByDelayRiskScoreGreaterThanEqualOrderByDelayRiskScoreDesc(50))
                .thenReturn(List.of(prediction(other, 90), prediction(shipment, 60)));

        List<EtaResponse> atRisk = etaService.getAtRisk(50);

        assertEquals(1, atRisk.size(), "the rival's shipment must not appear");
        assertEquals(42L, atRisk.get(0).getShipmentId());
    }

    @Test
    @DisplayName("an administrator sees every at-risk shipment")
    void administratorSeesEverything() {
        Shipment other = Shipment.builder()
                .id(99L).trackingNumber("STP0000000099")
                .createdBy(owner).status(ShipmentStatus.OUT_FOR_DELIVERY).build();

        when(currentUserService.getCurrentUser())
                .thenReturn(user(3L, Role.ADMINISTRATOR, "Root Admin"));
        when(etaPredictionRepository
                .findByDelayRiskScoreGreaterThanEqualOrderByDelayRiskScoreDesc(50))
                .thenReturn(List.of(prediction(other, 90), prediction(shipment, 60)));

        assertEquals(2, etaService.getAtRisk(50).size());
    }

    @Test
    @DisplayName("delivered and cancelled shipments drop off the at-risk list")
    void terminalShipmentsAreExcluded() {
        Shipment delivered = Shipment.builder()
                .id(55L).trackingNumber("STP0000000055")
                .createdBy(owner).status(ShipmentStatus.DELIVERED).build();
        Shipment cancelled = Shipment.builder()
                .id(56L).trackingNumber("STP0000000056")
                .createdBy(owner).status(ShipmentStatus.CANCELLED).build();

        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(etaPredictionRepository
                .findByDelayRiskScoreGreaterThanEqualOrderByDelayRiskScoreDesc(50))
                .thenReturn(List.of(prediction(delivered, 80), prediction(cancelled, 70),
                        prediction(shipment, 60)));

        List<EtaResponse> atRisk = etaService.getAtRisk(50);

        assertEquals(1, atRisk.size());
        assertEquals(42L, atRisk.get(0).getShipmentId());
    }

    @Test
    @DisplayName("a hostile risk floor is clamped instead of trusted")
    void riskFloorIsClamped() {
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(etaPredictionRepository
                .findByDelayRiskScoreGreaterThanEqualOrderByDelayRiskScoreDesc(anyInt()))
                .thenReturn(List.of());

        etaService.getAtRisk(-40);
        etaService.getAtRisk(9999);

        verify(etaPredictionRepository).findByDelayRiskScoreGreaterThanEqualOrderByDelayRiskScoreDesc(0);
        verify(etaPredictionRepository).findByDelayRiskScoreGreaterThanEqualOrderByDelayRiskScoreDesc(100);
    }

    @Test
    @DisplayName("a write-path refresh failure is swallowed, not propagated")
    void refreshQuietlySwallowsFailures() {
        when(shipmentRepository.findById(42L))
                .thenThrow(new IllegalStateException("database down"));

        assertDoesNotThrow(() -> etaService.refreshQuietly(42L));
    }

    @Test
    @DisplayName("a missing shipment is a 404, not a null forecast")
    void missingShipmentIsNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(shipmentRepository.findById(404L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class,
                        () -> etaService.getForShipment(404L)).getStatusCode());
    }

    @Test
    @DisplayName("the scheduled sweep keeps going when one shipment fails")
    void scheduledSweepIsResilient() {
        Shipment broken = Shipment.builder().id(500L).status(ShipmentStatus.IN_TRANSIT).build();
        when(shipmentRepository.findByStatusIn(any())).thenReturn(List.of(broken, shipment));
        when(routeRepository.findByShipmentIdOrderByLegNumberAsc(500L))
                .thenThrow(new IllegalStateException("route lookup failed"));

        assertEquals(1, etaService.recalculateActive(), "the healthy shipment is still updated");
    }
}
