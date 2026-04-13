package com.example.reminder.service;

import com.example.reminder.dto.notification.SendNotificationRequest;

public interface NotificationService {

    void send(SendNotificationRequest request);
}
