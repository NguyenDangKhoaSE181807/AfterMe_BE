package com.example.reminder.application.port.out;

import com.example.reminder.domain.model.NotificationMessage;

public interface NotificationGateway {

    void send(NotificationMessage message);
}




