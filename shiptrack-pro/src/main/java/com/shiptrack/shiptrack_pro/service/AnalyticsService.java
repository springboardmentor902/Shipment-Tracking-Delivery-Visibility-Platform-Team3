package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.analytics.AdminAnalyticsResponse;
import com.shiptrack.shiptrack_pro.dto.analytics.BusinessAnalyticsResponse;
import com.shiptrack.shiptrack_pro.dto.analytics.CustomerAnalyticsResponse;

public interface AnalyticsService {

    CustomerAnalyticsResponse getCustomerAnalytics(String customerEmail);

    BusinessAnalyticsResponse getBusinessAnalytics(String businessEmail);

    AdminAnalyticsResponse getAdminAnalytics();
}
