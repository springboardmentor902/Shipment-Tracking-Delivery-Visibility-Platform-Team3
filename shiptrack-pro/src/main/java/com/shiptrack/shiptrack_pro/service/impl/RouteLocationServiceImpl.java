package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.LocationRequest;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.service.RouteLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RouteLocationServiceImpl implements RouteLocationService {

    private final RouteRepository routeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void updateLocation(Long routeId, LocationRequest request) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Route not found with id: " + routeId
                ));

        // Save last known location
        route.setLastKnownLatitude(request.getLatitude());
        route.setLastKnownLongitude(request.getLongitude());

        routeRepository.save(route);

        // Broadcast location to the shipment-specific topic
        Long shipmentId = route.getShipment().getId();

        messagingTemplate.convertAndSend(
                "/topic/shipment/" + shipmentId,
                request
        );
    }
}