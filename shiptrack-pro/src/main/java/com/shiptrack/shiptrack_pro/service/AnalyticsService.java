package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.AnalyticsResponse;

import com.shiptrack.shiptrack_pro.entity.User;

public interface AnalyticsService {

    AnalyticsResponse getCustomerAnalytics(User user);

    AnalyticsResponse getBusinessAnalytics(User user);

    AnalyticsResponse getAdminAnalytics();
}