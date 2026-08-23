package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.LocationRequest;

public interface RouteLocationService {

    void updateLocation(Long routeId, LocationRequest request);
}
