package com.example.reminder.service.impl.notification;

import com.example.reminder.domain.model.NotificationMessage;

public interface NotificationSender {

    void send(NotificationMessage message);
}
