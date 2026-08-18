package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.ActiveDeliveryResponse;

import java.util.List;

public interface MonitoringService {

    List<ActiveDeliveryResponse> getActiveDeliveries();
}
