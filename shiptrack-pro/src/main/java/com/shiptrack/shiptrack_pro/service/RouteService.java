package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.dto.RouteUpdateRequest;

import java.util.List;

public interface RouteService {

    /** Create one route leg. Operator (assigned to the shipment) or administrator only. */
    RouteResponse createRoute(RouteRequest request);

    /** Update a leg or reassign its driver. Operator (owning the work) or administrator only. */
    RouteResponse updateRoute(Long routeId, RouteUpdateRequest request);

    /**
     * Recalculate coordinates, distance, duration and traffic for one leg from the
     * Maps provider. Falls back to the stored values when Maps is unavailable.
     */
    RouteResponse refreshRouteFromMaps(Long routeId);

    /** All legs of a shipment in travel order. Readable by anyone allowed to see the shipment. */
    List<RouteResponse> getRoutes(Long shipmentId);

    /** Single leg, authorized through the shipment it belongs to. */
    RouteResponse getRoute(Long routeId);
}
