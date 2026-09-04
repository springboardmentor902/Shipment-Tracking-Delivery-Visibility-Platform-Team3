package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.entity.Notification;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;

import java.util.List;

public interface NotificationService {

    Notification send(
            String type,
            User user,
            Shipment shipment
    );

    List<Notification> getNotificationsForUser(User user);

    Notification markAsRead(Long notificationId, User user);
}